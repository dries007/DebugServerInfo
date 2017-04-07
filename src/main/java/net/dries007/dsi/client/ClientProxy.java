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

package net.dries007.dsi.client;

import net.dries007.dsi.CommonProxy;
import net.dries007.dsi.DebugServerInfo;
import net.dries007.dsi.network.Request;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.text.DecimalFormat;
import java.util.*;

/**
 * @author Dries007
 */
@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy
{
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("########0.000");

    static Configuration config;

    private Minecraft mc;

    private boolean cfgLeft;
    private boolean cfgAlways;

    private int counter;
    private int cfgMaxDims;

    private double meanTickTime;
    private Map<Integer, Double> dims;
    private int free;
    private int total;
    private int max;

    @Override
    public void preInit()
    {
        mc = Minecraft.getMinecraft();

        super.preInit();
    }

    @Override
    public void config(Configuration config)
    {
        ClientProxy.config = config;

        cfgLeft = config.getBoolean("left", Configuration.CATEGORY_CLIENT, false, "Display on the left instead the right");
        cfgAlways = config.getBoolean("always", Configuration.CATEGORY_CLIENT, false, "Always display the tps & mem, even when not in F3");
        cfgMaxDims = config.getInt("maxDims", Configuration.CATEGORY_CLIENT, 10, 0, Integer.MAX_VALUE, "The max amount of dims to display. If 0, only total is shown.");

        super.config(config);
    }

    @Override
    public void init()
    {
        super.init();
    }

    @Override
    public void handleData(double meanTickTime, Map<Integer, Double> map, int free, int total, int max)
    {
        this.meanTickTime = meanTickTime;
        this.free = free;
        this.total = total;
        this.max = max;

        List<Map.Entry<Integer, Double>> list = new LinkedList<>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<Integer, Double>>()
        {
            @Override
            public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2)
            {
                return o2.getValue().compareTo(o1.getValue()); // REVERSED ordering
            }
        });
        Map<Integer, Double> dims = new LinkedHashMap<>(Math.max(list.size(), cfgMaxDims));
        for (Map.Entry<Integer, Double> entry : list)
        {
            if (dims.size() >= cfgMaxDims) break;
            dims.put(entry.getKey(), entry.getValue());
        }
        this.dims = dims;
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event)
    {
        dims = null;
    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.world == null) return;
        if ((cfgAlways || mc.gameSettings.showDebugInfo) && --counter < 0)
        {
            counter = 20;
            DebugServerInfo.getSnw().sendToServer(new Request(counter));
        }
    }

    @SubscribeEvent
    public void drawTextEvent(RenderGameOverlayEvent.Text event)
    {
        if (cfgAlways && !mc.gameSettings.showDebugInfo)
            draw(cfgLeft ? event.getLeft() : event.getRight(), Side.CLIENT);
        if (cfgAlways || mc.gameSettings.showDebugInfo) draw(cfgLeft ? event.getLeft() : event.getRight(), Side.SERVER);
    }

    private void draw(ArrayList<String> list, Side side)
    {
        int max = this.max;
        int total = this.total;
        int free = this.free;
        if (side.isClient())
        {
            if (list.isEmpty()) list.add("Client");
            max = (int) (Runtime.getRuntime().maxMemory() / 1024 / 1024);
            total = (int) (Runtime.getRuntime().totalMemory() / 1024 / 1024);
            free = (int) (Runtime.getRuntime().freeMemory() / 1024 / 1024);
            if (!mc.gameSettings.showDebugInfo) list.add(String.format("%d FPS", Minecraft.getDebugFPS()));
        }
        else
        {
            if (dims == null)
            {
                list.add("No server data :(");
                return;
            }
            if (!list.isEmpty()) list.add("Server");
        }

        int diff = total - free;
        list.add(String.format("Mem: % 2d%% %03d/%03dMB", diff * 100 / max, diff, max));
        list.add(String.format("Allocated: % 2d%% %03dMB", total * 100 / max, total));
        if (side.isClient()) return;

        list.add(String.format("Ticktime Overall: %sms (%d TPS)", DECIMAL_FORMAT.format(meanTickTime), (int) Math.min(1000.0 / meanTickTime, 20)));
        for (Map.Entry<Integer, Double> entry : dims.entrySet())
        {
            list.add(String.format("Dim %d: %sms (%d TPS)", entry.getKey(), DECIMAL_FORMAT.format(entry.getValue()), (int) Math.min(1000.0 / entry.getValue(), 20)));
        }
    }
}
