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

import io.netty.buffer.ByteBuf;
import net.dries007.dsi.ServerHelper;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import static net.dries007.dsi.DebugServerInfo.NBT_COUNTER;

/**
 * @author Dries007
 */
public class Request implements IMessage
{
    private int time;

    public Request(int time)
    {
        this.time = time;
    }

    @SuppressWarnings("unused")
    public Request()
    {

    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        time = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeInt(time);
    }

    public static class Handler implements IMessageHandler<Request, IMessage>
    {
        @Override
        public Data onMessage(Request message, MessageContext ctx)
        {
            ctx.getServerHandler().player.getEntityData().setInteger(NBT_COUNTER, message.time);
            return null;
        }
    }
}
