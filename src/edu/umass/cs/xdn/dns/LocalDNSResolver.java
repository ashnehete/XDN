package edu.umass.cs.xdn.dns;


import edu.umass.cs.xdn.XDNApp;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.dns.*;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

/**
 * This is  a local DNS (LDNS) resolver running on the edge server as a part of XDN agent.
 * It handles DNS request from end devices by taking the following steps:
 *
 * 1. LDNS checks local app state corresponding to the queried domain name
 * 2. If the app does not exist, or the app exists but is not running,
 * LDNS forwards the DNS query and do the name resolution on behalf of the client
 * 3. If the app is running, LDNS returns the IP address of the running app immediately
 *
 * @author gaozy
 */
public class LocalDNSResolver {

    static private XDNApp app;
    // set default TTL to 10s
    final static int TTL = 10;

    public LocalDNSResolver(XDNApp app) {
        this.app = app;

        final NioEventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group).channel(NioDatagramChannel.class)
                    .handler(new ChannelInitializer<NioDatagramChannel>() {
                        @Override
                        protected void initChannel(NioDatagramChannel nioDatagramChannel) throws Exception {
                            nioDatagramChannel.pipeline().addLast(new DatagramDnsQueryDecoder());
                            nioDatagramChannel.pipeline().addLast(new DatagramDnsResponseEncoder());
                            nioDatagramChannel.pipeline().addLast(new DnsHandler());
                        }
                    }).option(ChannelOption.SO_BROADCAST, true);

            ChannelFuture future = bootstrap.bind(53).sync();
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }

    @ChannelHandler.Sharable
    static class DnsHandler extends SimpleChannelInboundHandler<DatagramDnsQuery> {

        private byte[] convertIpStringToByteArray(String ip) throws UnknownHostException {
            return InetAddress.getByName(ip).getAddress();
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, DatagramDnsQuery query) throws UnsupportedEncodingException {
            // Keep the map in memory
            DatagramDnsResponse response = new DatagramDnsResponse(query.recipient(), query.sender(), query.id());
            try {
                DefaultDnsQuestion dnsQuestion = query.recordAt(DnsSection.QUESTION);
                response.addRecord(DnsSection.QUESTION, dnsQuestion);
                System.out.println("Domain：" + dnsQuestion.name());
                System.out.println("Query:"+query);

                String name = dnsQuestion.name();
                // FIXME: we only handle A type DNS query now
                if(dnsQuestion.type() != DnsRecordType.A) {
                    return;
                }

                ByteBuf buf = null;
                if (!app.nameExists(name) || !app.isRunning(name)) {
                    InetAddress[] addresses = InetAddress.getAllByName(name);
                    for(InetAddress address : addresses){
                        DefaultDnsRawRecord queryAnswer = new DefaultDnsRawRecord(dnsQuestion.name(), DnsRecordType.A, TTL,
                                Unpooled.wrappedBuffer(convertIpStringToByteArray(address.getHostAddress())));
                        response.addRecord(DnsSection.ANSWER, queryAnswer);
                    }
                } else {
                    try {
                        buf = Unpooled.wrappedBuffer(convertIpStringToByteArray(app.getIpAddrForDomainName(name)));
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                }

                if (buf != null) {
                    DefaultDnsRawRecord queryAnswer = new DefaultDnsRawRecord(dnsQuestion.name(), DnsRecordType.A, TTL, buf);
                    response.addRecord(DnsSection.ANSWER, queryAnswer);
                }
                // else, no record in the response

            } catch (Exception e) {
                System.out.println("Error：" + e);
                e.printStackTrace();
            }finally {
                ctx.writeAndFlush(response);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
        }
    }
}
