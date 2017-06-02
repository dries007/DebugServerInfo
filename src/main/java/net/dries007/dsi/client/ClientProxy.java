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
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.*;

import static net.minecraftforge.common.config.Configuration.CATEGORY_CLIENT;

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
    private int cfgMaxDims;
    private boolean cfgClock24h;
    private int cfgClockIRL;
    private int cfgClockMC;
    private int cfgClockMCDays;
    private int cfgModeFPS;
    private int cfgModeTPS;
    private int cfgModeRAM_Client;
    private int cfgModeRAM_Server;

    private int counter;

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

        config.setCategoryComment(CATEGORY_CLIENT, "For the mode config options this applies:\n0 = Never, 1 = Always, 2 = Only when not in F3");

        cfgLeft = config.getBoolean("left", CATEGORY_CLIENT, false, "Display on the left instead the right");
        cfgClock24h = config.getBoolean("clock24h", CATEGORY_CLIENT, true, "Make clocks 24h");
        cfgMaxDims = config.getInt("maxDims", CATEGORY_CLIENT, 5, 0, Integer.MAX_VALUE, "The max amount of dims to display. If 0, only total is shown.");

        cfgClockIRL = config.getInt("ClockIRL", CATEGORY_CLIENT, 1, 0, 3, "Clock, IRL time 0 = Never, 1 = Always, 2 = In F3, 3 = NOT in F3");
        cfgClockMC = config.getInt("ClockMC", CATEGORY_CLIENT, 1, 0, 3, "Clock, Minecraft time 0 = Never, 1 = Always, 2 = In F3, 3 = NOT in F3");
        cfgClockMCDays = config.getInt("ClockMCDays", CATEGORY_CLIENT, 1, 0, 3, "Minecraft days counter 0 = Never, 1 = Always, 2 = In F3, 3 = NOT in F3");

        cfgModeFPS = config.getInt("ModeFPS", CATEGORY_CLIENT, 1, 0, 3, "FPS mode (Frames per second, Client side) 0 = Never, 1 = Always, 2 = In F3, 3 = NOT in F3");
        cfgModeRAM_Client = config.getInt("ModeRAM_Client", CATEGORY_CLIENT, 1, 0, 3, "RAM usage mode, Client side 0 = Never, 1 = Always, 2 = In F3, 3 = NOT in F3");

        cfgModeTPS = config.getInt("ModeTPS", CATEGORY_CLIENT, 1, 0, 3, "TPS mode (Tick per second, Server Side) 0 = Never, 1 = Always, 2 = In F3, 3 = NOT in F3");
        cfgModeRAM_Server = config.getInt("ModeRAM_Server", CATEGORY_CLIENT, 1, 0, 3, "RAM usage mode, Server side 0 = Never, 1 = Always, 2 = In F3, 3 = NOT in F3");

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

    private boolean show(int mode)
    {
        switch (mode)
        {
            case 1:
                return true;
            case 2:
                return mc.gameSettings.showDebugInfo;
            case 3:
                return !mc.gameSettings.showDebugInfo;
            default:
                return false;
        }
    }

    @SubscribeEvent
    public void clientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END) return;
        if (mc.world == null) return;
        if ((show(cfgModeTPS) || show(cfgModeRAM_Server)) && --counter < 0)
        {
            counter = 20;
            DebugServerInfo.getSnw().sendToServer(new Request(counter));
        }
    }

    @SubscribeEvent
    public void drawTextEvent(RenderGameOverlayEvent.Text event)
    {
        ArrayList<String> list = cfgLeft ? event.getLeft() : event.getRight();

        World world = Minecraft.getMinecraft().world;

        if (world != null)
        {
            StringBuilder mc = new StringBuilder();

            if (show(cfgClockMCDays))
            {
                mc.append("Day ").append(world.getTotalWorldTime() / 24000);
            }

            if (show(cfgClockMC))
            {
                if (mc.length() > 0) mc.append(' ');

                // 0 -> 24000
                int time = (int) world.getWorldTime();
                // minutes = time % 1000
                // hours = (6 + time / 1000) % 24, cause 0 = 6h, max = 24h
                mc.append(doTime((6 + (time / 1000)) % 24, (int) ((time % 1000)*0.06), "MC"));
            }

            if (mc.length() > 0)
            {
                list.add(mc.toString());
            }

            if (show(cfgClockIRL))
            {
                Calendar cal = world.getCurrentDate();
                list.add(doTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), "IRL"));
            }
        }

        if (show(cfgModeFPS))
        {
            list.add(String.format("%d FPS", Minecraft.getDebugFPS()));
        }

        if (show(cfgModeRAM_Client))
        {
            drawRAM(list, Side.CLIENT);
        }

        if (show(cfgModeRAM_Server))
        {
            drawRAM(list, Side.SERVER);
        }

        if (show(cfgModeTPS))
        {
            if (dims == null)
            {
                list.add("No server TPS data :(");
            }
            else
            {
                list.add(String.format("Ticktime Overall: %sms (%d TPS)", DECIMAL_FORMAT.format(meanTickTime), (int) Math.min(1000.0 / meanTickTime, 20)));
                for (Map.Entry<Integer, Double> entry : dims.entrySet())
                {
                    list.add(String.format("Dim %d: %sms (%d TPS)", entry.getKey(), DECIMAL_FORMAT.format(entry.getValue()), (int) Math.min(1000.0 / entry.getValue(), 20)));
                }
            }
        }
    }

    private String doTime(int h, int m, String suffix)
    {
        if (cfgClock24h)
        {
            return String.format("%02d:%02d %s", h, m, suffix);
        }
        else
        {
            /*
             * American time: complex mess ahead.
             * am - pm goes:
             * 11:59 AM
             * 12:00 PM
             * 12:01 PM
             * 12:59 PM
             *  1:00 PM
             *  1:01 PM
             */
            int retardedH = h % 12;
            if (retardedH == 0) retardedH = 12;
            return String.format("%d:%02d %s %s", retardedH, m, h < 12 ? "AM" : "PM", suffix);
        }
    }

    private void drawRAM(ArrayList<String> list, Side side)
    {
        int max;
        int total;
        int free;
        if (side.isClient())
        {
            list.add("Client");
            max = (int) (Runtime.getRuntime().maxMemory() / 1024 / 1024);
            total = (int) (Runtime.getRuntime().totalMemory() / 1024 / 1024);
            free = (int) (Runtime.getRuntime().freeMemory() / 1024 / 1024);
        }
        else
        {
            max = this.max;
            total = this.total;
            free = this.free;
            if (dims == null)
            {
                list.add("No server RAM data :(");
                return;
            }
            list.add("Server");
        }

        int diff = total - free;
        list.add(String.format("Mem: % 2d%% %03d/%03dMB", diff * 100 / max, diff, max));
        list.add(String.format("Allocated: % 2d%% %03dMB", total * 100 / max, total));
    }
}
