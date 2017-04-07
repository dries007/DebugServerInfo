/*
 * Copyright (c) 2017 Dries K. aka Dries007
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.dries007.dsi.network;

import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.map.TIntDoubleMap;
import io.netty.buffer.ByteBuf;
import net.dries007.dsi.DebugServerInfo;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Dries007
 */
public class Data implements IMessage
{
    private double meanTickTime;
    private double meanTPS;
    private TIntDoubleMap tMap;
    private Map<Integer, Double> jMap;
    private int free;
    private int total;
    private int max;

    @SuppressWarnings("unused")
    public Data()
    {

    }

    public Data(double meanTickTime, TIntDoubleMap map, int free, int total, int max)
    {
        this.meanTickTime = meanTickTime;
        this.tMap = map;
        this.free = free;
        this.total = total;
        this.max = max;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        meanTickTime = buf.readDouble();
        int len = buf.readInt();
        jMap = new HashMap<>(len);
        while (len-- != 0)
        {
            jMap.put(buf.readInt(), buf.readDouble());
        }
        free = buf.readInt();
        total = buf.readInt();
        max = buf.readInt();
    }

    @Override
    public void toBytes(final ByteBuf buf)
    {
        buf.writeDouble(meanTickTime);
        buf.writeInt(tMap.size());
        TIntDoubleIterator i = tMap.iterator();
        while (i.hasNext())
        {
            i.advance();
            buf.writeInt(i.key());
            buf.writeDouble(i.value());
        }
        buf.writeInt(free);
        buf.writeInt(total);
        buf.writeInt(max);
    }

    public static class Handler implements IMessageHandler<Data, IMessage>
    {
        @Override
        public IMessage onMessage(Data message, MessageContext ctx)
        {
//            DebugServerInfo.getLogger().info("Got data packet");
            DebugServerInfo.getProxy().handleData(message.meanTickTime, message.jMap, message.free, message.total, message.max);
            return null;
        }
    }
}
