package com.xiaoyu.interview.config;

// 流式数据处理回调接口
public interface StreamDataCallback {
    /**
     * 处理每个data事件的内容
     * @param data JSON字符串（如：{"content":"msg","node_is_finish":false,...}）
     */
    void onData(String data);

    /**
     * 流结束时回调（可选）
     */
    default void onComplete() {}

    /**
     * 发生错误时回调（可选）
     * @param throwable 异常信息
     */
    default void onError(Throwable throwable) {}
}