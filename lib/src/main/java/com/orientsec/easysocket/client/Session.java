package com.orientsec.easysocket.client;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orientsec.easysocket.Address;
import com.orientsec.easysocket.PacketHandler;
import com.orientsec.easysocket.Period;

import java.net.InetAddress;

public interface Session extends PacketHandler {

    /**
     * 是否连接
     *
     * @return 是否连接
     */
    boolean isConnect();

    /**
     * 连接是否可达
     *
     * @return 是否可达
     */
    boolean isAvailable();

    /**
     * 服务器是否可用。Session在网络可用的情况下，成功进入{@link State#AVAILABLE}，则认为服务器可用。
     *
     * @return 服务器是否可用。
     */
    boolean isServerAvailable();

    /**
     * 获取当前连接站点信息
     *
     * @return 当前连接站点信息
     */
    @NonNull
    Address getAddress();

    @Nullable
    InetAddress getInetAddress();

    /**
     * 连接时间，单位ms。
     *
     * @return socket连接花费的时间。
     */
    long connectTime();

    /**
     * 各阶段连接时间，单位ms。
     *
     * @return socket连接花费的时间。
     */
    long connectTime(Period period);
}
