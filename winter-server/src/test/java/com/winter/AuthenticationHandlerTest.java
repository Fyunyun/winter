// package com.winter; // 包名，确保测试类位于 com.winter 包下

// import io.netty.channel.ChannelInboundHandlerAdapter; // Netty 入站处理器基类
// import io.netty.channel.ChannelHandlerContext;        // Netty Handler 上下文对象
// import io.netty.channel.embedded.EmbeddedChannel;     // Netty 提供的内存管道，用于单元测试
// import org.junit.jupiter.api.AfterEach;               // JUnit5：每个测试后执行
// import org.junit.jupiter.api.BeforeEach;              // JUnit5：每个测试前执行
// import org.junit.jupiter.api.Test;                    // JUnit5：标记测试方法

// import static org.junit.jupiter.api.Assertions.*;     // JUnit5：断言工具

// /**
//  * AuthenticationHandler 的单元测试
//  * 使用 Netty 提供的 EmbeddedChannel 进行测试（无需真实网络连接）
//  * 通过模拟入站/出站事件，验证认证逻辑是否正确工作
//  */
// class AuthenticationHandlerTest {

//     // 用于承载待测 Handler 管线的内存通道，替代真实网络通道
//     private EmbeddedChannel channel;

//     /**
//      * 辅助下游 Handler，用于捕获被 fireChannelRead 转发过来的消息
//      * 只保存收到的第一条消息，便于断言
//      */
//     private static class TestDownstreamHandler extends ChannelInboundHandlerAdapter {
//         // 保存收到的消息（若未收到则为 null）
//         private Object receivedMessage;

//         @Override
//         public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//             // 捕获上游转发的消息（例如 AuthenticationHandler 调用 fireChannelRead 的消息）
//             this.receivedMessage = msg;
//             // 继续传播到下一个入站处理器（若有）
//             super.channelRead(ctx, msg);
//         }

//         // 获取捕获的消息
//         public Object getReceivedMessage() {
//             return receivedMessage;
//         }

//         // 清空已捕获的消息（在每次测试结束时调用）
//         public void clear() {
//             receivedMessage = null;
//         }
//     }

//     // 测试中作为下游处理器使用的实例
//     private TestDownstreamHandler downstreamHandler;

//     @BeforeEach
//     void setUp() {
//         // 初始化下游处理器
//         downstreamHandler = new TestDownstreamHandler();
//         // 构建管线：AuthenticationHandler -> TestDownstreamHandler
//         // 认证成功时应将消息继续传递到 downstreamHandler；失败时不传递
//         channel = new EmbeddedChannel(new com.winter.core.AuthenticationHandler(), downstreamHandler);
//     }

//     @AfterEach
//     void tearDown() {
//         if (channel != null) {
//             // 结束通道生命周期：刷新并释放资源（适用于 EmbeddedChannel）
//             channel.finish();
//         }
//         if (downstreamHandler != null) {
//             // 清理下游处理器的状态，避免测试间相互影响
//             downstreamHandler.clear();
//         }
//     }

//     /**
//      * 测试认证成功场景
//      * 期望：消息被转发给下游 Handler，通道保持打开，没有向客户端写回任何消息
//      */
//     @Test
//     void testAuthenticationSuccess() {
//         // 模拟一条符合认证规则的消息（具体规则由 AuthenticationHandler 决定，示例为以 "auth " 开头）
//         String authMessage = "auth my-secret-token";

//         // 模拟客户端发送入站消息（触发 channelRead）
//         channel.writeInbound(authMessage);

//         // 下游应当接收到同一条消息（说明 AuthenticationHandler 成功调用 fireChannelRead）
//         assertEquals(authMessage, downstreamHandler.getReceivedMessage());

//         // 认证成功逻辑不应产生任何出站响应
//         assertNull(channel.readOutbound());

//         // 通道保持开启（未被关闭）
//         assertTrue(channel.isOpen());
//     }

//     /**
//      * 测试认证失败场景
//      * 期望：向下游不转发消息，向客户端写回失败提示，并关闭连接
//      */
//     @Test
//     void testAuthenticationFailure() {
//         // 非认证消息（例如不以 "auth " 开头）
//         String badMessage = "hello world";

//         // 写入入站消息，触发认证失败
//         channel.writeInbound(badMessage);

//         // 认证失败时不应向下游传播消息（没有 fireChannelRead）
//         assertNull(downstreamHandler.getReceivedMessage());

//         // 检查出站响应（认证失败提示）
//         // 说明：如果未在管线中添加 StringEncoder，出站对象类型仍为 String
//         String response = channel.readOutbound();
//         assertEquals("认证失败，请重新连接。\n", response);

//         // 不应有额外的出站消息
//         assertNull(channel.readOutbound());

//         // 通道应被关闭（认证失败后主动断开连接）
//         assertFalse(channel.isOpen());
//     }

//     /**
//      * 额外测试：多次失败（确保每次都独立处理）
//      * 首次失败后通道关闭，后续入站写入返回 false（写入失败）
//      */
//     @Test
//     void testMultipleFailures() {
//         // 第一次失败，产生失败提示并关闭通道
//         channel.writeInbound("bad1");
//         String resp1 = channel.readOutbound();
//         assertEquals("认证失败，请重新连接。\n", resp1);
//         assertFalse(channel.isOpen());

//         // 通道已关闭，后续写操作应返回 false（无法再写入）
//         assertFalse(channel.writeInbound("bad2"));
//     }
// }