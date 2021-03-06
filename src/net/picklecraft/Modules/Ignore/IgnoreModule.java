package net.picklecraft.Modules.Ignore;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import net.picklecraft.Modules.IModule;
import net.picklecraft.PickleCraftPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.libs.com.google.gson.stream.JsonReader;
import org.bukkit.craftbukkit.libs.com.google.gson.stream.JsonWriter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Copyright (c) 2011-2014
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * @author Pickle
 *
 * Currently broke..
 */
public class IgnoreModule implements IModule, Listener {
    
    private final long SAVE_PERIOD = 1200L; //1 minute
    
    public List<IgnorePlayer> playerIgnoreList = new ArrayList<>();

    private final PickleCraftPlugin plugin;

    private IgnorePlayerListener igPlayerListener;

    private final File igFile;

    public IgnoreModule(PickleCraftPlugin plugin) {
        this.plugin = plugin;
        igFile = new File(plugin.getDataFolder() + "/ignores.json");
        if (!igFile.exists()) {
            try {
                igFile.createNewFile();
            } catch (IOException ex) {
                PickleCraftPlugin.log.log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public PickleCraftPlugin getPlugin() {
        return plugin;
    }

    @Override
    public String getName() {
        return "Ignore";
    }

    @Override
    public void onDisable() {
        Save();
    }

    @Override
    public void onEnable() {
        igPlayerListener = new IgnorePlayerListener(this);
        plugin.getServer().getPluginManager().registerEvents(igPlayerListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        Load();
        
        //Automatic save after time
        new BukkitRunnable() {
 
            @Override
            public void run() {
                Save();
            }
 
        }.runTaskTimer(plugin, 0L, SAVE_PERIOD);
    }

    @Override
    public void sendCommandList(CommandSender sender) {
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }
        if (player != null) {
            Command c = plugin.getCommand("ignore");
            if (PickleCraftPlugin.hasPerm(player, c.getPermission())) {
                player.sendMessage(
                        plugin.getStringFromConfig("ignorecraft.messages.commandhelplist.header")
                );
                player.sendMessage(c.getUsage() + " " + c.getDescription());
                c = plugin.getCommand("ignoreall");
                player.sendMessage(c.getUsage() + " " + c.getDescription());
                c = plugin.getCommand("ignorelist");
                player.sendMessage(c.getUsage() + " " + c.getDescription());
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
            String label, String[] args) {
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }
        /*
         * ignore command.
         */
        if (command.getName().equalsIgnoreCase("ignore")) {
            if (player != null) {
                if (PickleCraftPlugin.hasPerm(player, command.getPermission())) {
                    if (args.length >= 1) {
                        if (args.length >= 2) {
                            /*
                             * flags
                             */
                            if (args[0].equalsIgnoreCase("-r")) { // remove ignore flag
                                Object[] playerAndbool = PickleCraftPlugin.getPlayer(args[1]);
                                Player p = (Player) playerAndbool[0];
                                if (p != null) {
                                    if ((Boolean) playerAndbool[1] == false) {
                                        IgnorePlayer igP = getIgnorePlayer(player);
                                        if (igP != null) {
                                            igP.unignorePlayer(p.getUniqueId(), true);
                                        } else {
                                            player.sendMessage(
                                                    plugin.getStringFromConfig("ignorecraft.messages.errors.isnotignored", p.getName())
                                            );
                                        }
                                    } else {
                                        player.sendMessage(
                                                plugin.getStringFromConfig("common.messages.errors.tomanyplayers", args[1])
                                        );
                                    }
                                    return true;
                                } else {
                                    player.sendMessage(
                                            plugin.getStringFromConfig("common.messages.errors.playerdontexist", args[1])
                                    );
                                }
                            }
                        } else {
                            Object[] playerAndbool = PickleCraftPlugin.getPlayer(args[0]);
                            Player p = (Player) playerAndbool[0];
                            if (p != null) {
                                if ((Boolean) playerAndbool[1] == false) {
                                    IgnorePlayer igP = getIgnorePlayer(player);
                                    if (igP != null) {
                                        igP.ignorePlayer(p.getUniqueId(), true);
                                    } else {
                                        igP = new IgnorePlayer(this, player.getUniqueId());
                                        igP.ignorePlayer(p.getUniqueId(), true);
                                        playerIgnoreList.add(igP);
                                    }
                                } else {
                                    player.sendMessage(
                                            plugin.getStringFromConfig("common.messages.errors.tomanyplayers", args[0])
                                    );
                                }
                            } else {
                                player.sendMessage(
                                        plugin.getStringFromConfig("common.messages.errors.playerdontexist", args[0])
                                );
                            }
                            return true;
                        }
                    }
                } else {
                    player.sendMessage(
                            plugin.getStringFromConfig("common.messages.errors.donthaveperm")
                    );
                    return true;
                }
            } else {
                sender.sendMessage("This is a player only command.");
                return true;
            }
        } /*
         * ignoreall command
         */ else if (command.getName().equalsIgnoreCase("ignoreall")) { //all ignore
            if (player != null) {
                if (PickleCraftPlugin.hasPerm(player, command.getPermission())) {
                    IgnorePlayer igP = getIgnorePlayer(player);
                    if (igP != null) {
                        igP.toggleAllIgnore(true);
                    } else {
                        igP = new IgnorePlayer(this, player.getUniqueId());
                        igP.toggleAllIgnore(true);
                        playerIgnoreList.add(igP);
                    }
                } else {
                    player.sendMessage(
                            plugin.getStringFromConfig("common.messages.errors.donthaveperm")
                    );
                }
            } else {
                sender.sendMessage("This is a player only command.");
            }
            return true;
        } 
         /*
         * display ignore list
         */ 
         else if (command.getName().equalsIgnoreCase("ignorelist")) { //displays ignore list
            if (player != null) {
                IgnorePlayer igP = getIgnorePlayer(player);
                if (igP != null) {
                    listIgnores(igP);
                }
            } else {
                sender.sendMessage("This is a player only command.");
            }
            return true;
        }
        return false;
    }

    public IgnorePlayer getIgnorePlayer(Player player) {
        for (IgnorePlayer igp : playerIgnoreList) {
            if (igp.getPlayer() == player) {
                return igp;
            }
        }
        return null;
    }

    public void listIgnores(IgnorePlayer player) {
        if (player.isAllIgnored()) {
            player.getPlayer().sendMessage(
                    plugin.getStringFromConfig("ignorecraft.messages.info.ignoreall")
            );
        } else {
            ArrayList<UUID> ignores = (ArrayList<UUID>) player.getIgnoreList();
            if (ignores.size() <= 0) {
                player.getPlayer().sendMessage(
                        plugin.getStringFromConfig("ignorecraft.messages.errors.noignores")
                );
            } else {
                StringBuilder s = new StringBuilder();
                s.append("&2Ignoring: ");
                for (int i = 0; i < ignores.size(); i++) {
                    Player p = plugin.getServer().getPlayer(ignores.get(i));
                    s.append("&e");
                    s.append(p.getName());
                    s.append(", ");
                    if (i % 8 == 0 && i != 0) {
                        player.getPlayer().sendMessage(PickleCraftPlugin.Colorize(s.toString()));
                        s = new StringBuilder();
                    }
                }
                String d = s.toString();
                if (!d.isEmpty()) {
                    //incase not enough indexs to fire the sendmessage in le loop :c
                    player.getPlayer().sendMessage(PickleCraftPlugin.Colorize(d));
                }
            }
        }
    }

    public void Save() {
        try (FileWriter fw = new FileWriter(igFile); JsonWriter writer = new JsonWriter(fw)) {
            writer.setIndent(" ");
            writer.beginArray(); //begin players array
            for (IgnorePlayer player : playerIgnoreList) {
                writer.beginObject(); //begin player object
                writer.name("uuid").value(player.getUUID().toString());
                writer.name("ignoreall").value(player.isAllIgnored());
                writer.name("ignores");
                writer.beginArray(); //begin ignore array
                for (UUID uuid : player.getIgnoreList()) {
                    writer.beginObject(); //begin ignored player object
                    writer.name("uuid").value(uuid.toString());
                    writer.endObject(); //end ignored player object
                }
                writer.endArray(); //end ignore array

                writer.endObject(); //end player object
            }
            writer.endArray(); //end players array
        } catch (IOException e) {
            PickleCraftPlugin.log.warning(e.getMessage());
        }
    }

    public void Load() {
        IgnorePlayer player = null;
        try (FileReader fr = new FileReader(igFile); JsonReader reader = new JsonReader(fr)) {
            reader.beginArray(); //begin players array
            while (reader.hasNext()) {
                reader.beginObject(); //begin player object
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    
                    if (name.equalsIgnoreCase("uuid")) {
                        UUID uuid = UUID.fromString(reader.nextString());
                        player = new IgnorePlayer(this, uuid);
                    } 
                    
                    else if (name.equalsIgnoreCase("ignoreall")) {
                        if (reader.nextBoolean()) {
                            player.toggleAllIgnore(false);
                        }
                    } 
                    
                    else if (name.equalsIgnoreCase("ignores")) {
                        reader.beginArray(); //begin ignores array
                        while (reader.hasNext()) {
                            reader.beginObject(); //begin ignored player object
                            String n = reader.nextName();
                            if (n.equalsIgnoreCase("uuid")) {
                                UUID ignoredUuid = UUID.fromString(reader.nextString());
                                if (ignoredUuid != null) {
                                    player.ignorePlayer(ignoredUuid, false);
                                }
                            }
                            reader.endObject(); //end ignored player object
                        }
                        reader.endArray(); //end ignores array
                    }
                    
                }
                reader.endObject(); //end player object
                playerIgnoreList.add(player);
            }
            reader.endArray(); //end players array
        } catch (EOFException optional) {
            //Ignore.
        } catch (IOException e) {
            PickleCraftPlugin.log.warning(e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onWorldSave(WorldSaveEvent event) {
        //save json when the world does.
        Save();
    }
}
