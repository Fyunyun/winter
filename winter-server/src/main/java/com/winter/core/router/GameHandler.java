package com.winter.core.router;

import com.winter.msg.MsgId.CmdId;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个方法为消息处理器
 */
@Target(ElementType.METHOD) // 只能用在方法上
@Retention(RetentionPolicy.RUNTIME) // 运行时保留，方便反射读取
public @interface GameHandler {
    // 对应 id.proto 里的枚举
    CmdId cmd();
}