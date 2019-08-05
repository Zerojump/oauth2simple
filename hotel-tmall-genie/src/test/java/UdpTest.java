import com.cmy.hoteltmallgenie.cache.HotelCache;
import com.cmy.hoteltmallgenie.controller.CommandController;
import com.cmy.hoteltmallgenie.service.UdpCommandSender;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

/**
 * <p>@author chenmingyi
 * <p>@version 1.0
 * <p>date: 2019/8/4
 */
public class UdpTest {
    private static final int PORT = 5107;
    private static final String PROTOCOL_PREFIX = "yyxx";
    private static final String HOTEL_ID = "7days";
    private static final String ROOMNAME = "president1001";
    private static volatile EventLoopGroup serverGroup;
    private static volatile Channel serverChannel;
    private static volatile CountDownLatch latch;
    //private static Lock lock = new ReentrantLock();
    //private static Condition condition = lock.newCondition();

    @Test
    public void testUdpServer() throws InterruptedException, ExecutionException {
        initUdpServer();
    }

    private void initUdpServer() throws InterruptedException {
        Bootstrap b = new Bootstrap();
        EventLoopGroup group = new NioEventLoopGroup();
        b.group(group)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .handler(new SimpleChannelInboundHandler<DatagramPacket>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
                        ByteBuf buf = packet.copy().content();
                        byte[] req = new byte[buf.readableBytes()];
                        buf.readBytes(req);
                        String msg = new String(req, "UTF-8");

                        //InetSocketAddress remote = (InetSocketAddress) ctx.channel().remoteAddress();
                        System.out.println(String.format("msg from client:%s", msg));//打印收到的信息
                        //向客户端发送消息
                        // 由于数据报的数据是以字符数组传的形式存储的，所以传转数据
                        byte[] bytes = msg.getBytes("UTF-8");
                        DatagramPacket data = new DatagramPacket(Unpooled.copiedBuffer(bytes), packet.sender());
                        ctx.writeAndFlush(data);

                        //lock.lock();
                        //try {
                        //    condition.signal();
                        //} finally {
                        //    lock.unlock();
                        //}
                    }
                });

        Channel channel = b.bind(PORT).sync().channel();
        serverGroup= group;
        serverChannel = channel;
        latch.countDown();
        channel.closeFuture().await();
    }

    @Test
    public void testUdpClient() throws Exception {
        latch = new CountDownLatch(1);
        new Thread(() -> {
            try {
                initUdpServer();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        latch.await();

        UdpCommandSender sender = new UdpCommandSender("localhost", PORT);
        HotelCache skillStore = new HotelCache();
        skillStore.init();

        for (Map.Entry<String, String> entry : skillStore.getAllSkills()) {
            sender.send(CommandController.buildCtrlReq(PROTOCOL_PREFIX,HOTEL_ID,ROOMNAME, entry.getValue()));
            //lock.lock();
            //try {
            //    condition.await();
            //} finally {
            //    lock.unlock();
            //}
        }

        sender.close();
        serverGroup.shutdownGracefully();

    }
}
