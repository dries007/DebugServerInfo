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

import net.dries007.dsi.network.Data;
import net.dries007.dsi.network.Request;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Logger;

@Mod(
        modid = DebugServerInfo.MOD_ID,
        name = DebugServerInfo.MOD_NAME,
        version = DebugServerInfo.VERSION,
        acceptableRemoteVersions = "*",
        guiFactory = "net.dries007.dsi.client.ConfigGuiFactory"
)
public class DebugServerInfo
{
    public static final String MOD_ID = "debugserverinfo";
    public static final String MOD_NAME = "DebugServerInfo";
    public static final String VERSION = "1.0.0";
    public static final String NBT_COUNTER = MOD_ID + "Count";

    @Mod.Instance
    private static DebugServerInfo instance;

    @SidedProxy(clientSide = "net.dries007.dsi.client.ClientProxy", serverSide = "net.dries007.dsi.CommonProxy")
    private static CommonProxy proxy;
    private Logger logger;
    private SimpleNetworkWrapper snw;

    private Configuration config;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();

        config = new Configuration(event.getSuggestedConfigurationFile());
        doConfig();

        int id = 1; // Don't use 0, more easy debug.
        snw = NetworkRegistry.INSTANCE.newSimpleChannel(MOD_ID);

        snw.registerMessage(Request.Handler.class, Request.class, id++, Side.SERVER);
        snw.registerMessage(Data.Handler.class, Data.class, id++, Side.CLIENT);

        proxy.preInit();
    }

    @SubscribeEvent
    public void updateConfig(ConfigChangedEvent.OnConfigChangedEvent event)
    {
        if (event.getModID().equals(MOD_ID)) doConfig();
    }

    private void doConfig()
    {
        proxy.config(config);

        if (config.hasChanged()) config.save();
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(proxy);
        MinecraftForge.EVENT_BUS.register(ServerHelper.I);

        proxy.init();
    }

    public static Logger getLogger()
    {
        return instance.logger;
    }

    public static SimpleNetworkWrapper getSnw()
    {
        return instance.snw;
    }

    public static CommonProxy getProxy()
    {
        return proxy;
    }
}
