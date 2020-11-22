package dev.glassey;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class DeathSwap extends JavaPlugin implements Listener {
    private final Random random = new Random();
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player))
        {
            sender.sendMessage("Can't play from console.");
            return true;
        }
        Player p = (Player) sender;
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        
        switch(label.toLowerCase())
        {
            case "cancel":
            {
                Pair pair = Pair.getPair(p);
                if(pair==null)
                {
                    p.sendMessage("Nothing to cancel.");
                    return true;
                }
                else if (!pair.isPending())
                {
                    p.sendMessage("Can't cancel current game !");
                    return true;
                }
                Player other = pair.getOther(p);
                
                p.sendMessage("Request cancelled.");
                other.sendMessage(p.getName()+" cancelled the game request.");
                pair.clear();
                break;
            }
            case "startrandom":
            {
                List<Player> others = new ArrayList<>(Bukkit.getOnlinePlayers());
                others.remove(p);
                Bukkit.getOnlinePlayers().forEach(o -> {
                    if(Pair.hasPair(o)) {
                        others.remove(o);
                    }
                });
                
                if(others.size()==0)
                {
                    p.sendMessage("No available player.");
                    return true;
                }
                
                if(Pair.hasPair(p))
                {
                    p.sendMessage("You must first "+ChatColor.RED+"/cancel"+ChatColor.RESET+".");
                    return true;
                }
                Player other = others.get(random.nextInt(others.size()));
                requestStart(p, other);
                break;
            }
            case "start":
            {
                if(Pair.hasPair(p))
                {
                    p.sendMessage("You must first "+ChatColor.RED+"/cancel"+ChatColor.RESET+".");
                    return true;
                }
                if(args.length!=1)
                {
                    sender.sendMessage("You must specify exactly one player.");
                    sender.sendMessage("/start <player_name>");
                    return true;
                }
                if(p.getName().equalsIgnoreCase(args[0]))
                {
                    p.sendMessage("Can't play with yourself here :)");
                    return true;
                }
                for(Player other : online)
                {
                    if(other.getName().equalsIgnoreCase(args[0]))
                    {
                        requestStart(p, other);
                        return true;
                    }
                }
                sender.sendMessage("Can't find player with the name "+ChatColor.AQUA + args[0] + ChatColor.RESET+".");
                break;
            }
            case "accept":
            case "decline":
            {
                boolean accepted = label.equalsIgnoreCase("accept");
                
                Pair pair = Pair.getPair(p);
                if(pair==null)
                {
                    p.sendMessage("There is no pending request.");
                    return true;
                }
                if(!pair.isPending())
                {
                    p.sendMessage("You are already in game !");
                    return true;
                }
                if(!pair.isRecipient(p))
                {
                    p.sendMessage("You just sent this request, you can't "+(accepted?"accept":"decline")+" it for "+pair.getOther(p).getName()+"!");
                    return true;
                }
                proceed(pair, accepted);
                break;
            }
            default: {
                sender.sendMessage("Unknown error.");
                break;
            }
        }
        return true;
    }
    
    private void proceed(Pair p, boolean accepted) {
        if(!accepted)
        {
            p.getP1().sendMessage(p.getP2().getName()+" declined.");
            p.getP2().sendMessage("You declined the game request with "+p.getP1().getName()+".");
            p.clear();
            return;
        }
        p.accept();
        p.getP1().sendMessage(p.getP2().getName()+" accepted ! Starting game...");
        p.getP2().sendMessage("You accepted to play with "+p.getP1().getName()+" ! Starting game...");
        p.start();
    }
    
    public void requestStart(Player sender, Player other) {
        new Pair(this, sender, other);
        sender.sendMessage("Sending request to "+other.getName()+"...");
        other.sendMessage("Hey ! "+sender.getName()+" wants to play !");
        other.sendMessage("You can either "+ChatColor.DARK_GREEN+"/accept"+ChatColor.RESET+" or "+ChatColor.DARK_RED+"/decline"+ChatColor.RESET+"!");
    }
    

    @Override
    public void onDisable() {
    }
    
    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }
    
    @EventHandler
    public void onLogout(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        Pair pair = Pair.getPair(p);
        if(pair==null) return;
        if(pair.isPending())
        {
            proceed(pair, false); //Cancel the request
            return;
        }
        pair.end(null);
    }
    
    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if(event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) return;
        Player p = event.getPlayer();
        Pair pair = Pair.getPair(p);
        if(pair==null) return;
        if(pair.isPending()) return; //We don't care for now
        pair.end(pair.getOther(p));
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        Pair p  = Pair.getPair(dead);
        if(p==null) return;
        if(p.isPending()) return;
        Bukkit.broadcastMessage(event.getDeathMessage());
        event.setCancelled(true);
        dead.setHealth(20);
        p.end(p.getOther(dead));
    }
    
    @EventHandler
    public void onWorldInit(WorldInitEvent event)
    {
        event.getWorld().setKeepSpawnInMemory(false);
    }
}
