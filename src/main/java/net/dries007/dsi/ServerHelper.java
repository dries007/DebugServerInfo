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

package net.dries007.dsi;

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import net.dries007.dsi.network.Data;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import static net.dries007.dsi.DebugServerInfo.NBT_COUNTER;
import static net.dries007.dsi.DebugServerInfo.getLogger;

/**
 * @author Dries007
 */
public class ServerHelper
{
    public static final ServerHelper I = new ServerHelper();

    private static Data data;

    private ServerHelper()
    {
    }

    private static long mean(long[] values)
    {
        long sum = 0;
        for (long v : values) sum += v;
        return sum / values.length;
    }

    public static Data getData()
    {
        if (data != null) return data;

        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();

        Integer[] dimsObj = DimensionManager.getIDs();
        TIntDoubleMap map = new TIntDoubleHashMap(dimsObj.length);

        for (Integer dim : dimsObj)
        {
            map.put(dim, mean(server.worldTickTimes.get(dim)) * 1.0E-6D);
        }

        double meanTickTime = mean(server.tickTimeArray) * 1.0E-6D;

        int total = (int) (Runtime.getRuntime().totalMemory() / 1024 / 1024);
        int max = (int) (Runtime.getRuntime().maxMemory() / 1024 / 1024);
        int free = (int) (Runtime.getRuntime().freeMemory() / 1024 / 1024);

        data = new Data(meanTickTime, map, free, total, max);
        return data;
    }

    @SubscribeEvent
    public void onTickServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.START) return;
        data = null;
    }

    @SubscribeEvent
    public void onTickPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.side.isClient() || event.phase != TickEvent.Phase.START) return;
        int count = event.player.getEntityData().getInteger(NBT_COUNTER);
        if (count <= 0) return;
        event.player.getEntityData().setInteger(NBT_COUNTER, count - 1);
        Data data = ServerHelper.getData();
        if (data != null)
        {
            try
            {
                DebugServerInfo.getSnw().sendTo(data, (EntityPlayerMP) event.player);
            }
            catch (Exception e)
            {
                getLogger().info("Caught error in sendTo. {} ({})", e.getMessage(), e.getClass().getName());
            }
        }
    }
}
