package dev.glassey;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import se.hyperver.hyperverse.Hyperverse;
import se.hyperver.hyperverse.exception.HyperWorldCreationException;
import se.hyperver.hyperverse.world.HyperWorld;
import se.hyperver.hyperverse.world.WorldConfiguration;
import se.hyperver.hyperverse.world.WorldFeatures;
import se.hyperver.hyperverse.world.WorldType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

public class Pair {
    Plugin plugin;
    
    private final Random random = new Random();
    private Player p1;
    private Player p2;
    private boolean accepted = false;
    private HyperWorld world;
    private BukkitTask runTask;
    private BukkitTask s5TimerTask;
    private BukkitTask s10TimerTask;
    
    private static final Set<Pair> pairs = new HashSet<>();
    private final Set<Player> both = new HashSet<>();
    
    public Pair(@Nonnull Plugin plugin, @Nonnull Player p1, @Nonnull Player p2)
    {
        this.plugin = plugin;
        this.p1 = p1;
        this.p2 = p2;
        pairs.add(this);
        both.add(p1);
        both.add(p2);
    }
    
    public void clear() {
        if(runTask!=null && !runTask.isCancelled())
            runTask.cancel();
        if(s5TimerTask!=null && !s5TimerTask.isCancelled())
            s5TimerTask.cancel();
        if(s10TimerTask!=null && !s10TimerTask.isCancelled())
            s10TimerTask.cancel();
        this.p1 = null;
        this.p2 = null;
        pairs.remove(this);
    }
    
    public void accept() {
        this.accepted = true;
    }
    
    public boolean isPending() {
        return !this.accepted;
    }
    
    public Player getP1()
    {
        return this.p1;
    }
    
    public Player getP2()
    {
        return this.p2;
    }
    
    public boolean contains(Player p)
    {
        return (p1.equals(p) || p2.equals(p));
    }
    
    public Player getOther(Player p)
    {
        if(!contains(p)) return p;
        if(p.equals(p1)) return p2;
        return p1;
    }
    
    public boolean isSender(Player p)
    {
        return this.p1.equals(p);
    }
    
    public boolean isRecipient(Player p)
    {
        return this.p2.equals(p);
    }
    
    public static boolean hasPair(Player p)
    {
        return getPair(p)!=null;
    }
    
    @Nullable
    public static Pair getPair(Player p)
    {
        for(Pair pair : pairs) {
            if(pair.contains(p))
            {
                return pair;
            }
        }
        return null;
    }
    
    private final BukkitRunnable runTimer = new BukkitRunnable() {
        @Override
        public void run() {
            start10sTimer();
        }
    };
    
    private final BukkitRunnable s5Timer = new BukkitRunnable() {
        int seconds = 5;
        @Override
        public void run() {
            if(seconds >0) {
                both.forEach(p -> p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + seconds + "..."));
                seconds--;
            }
            else {
                both.forEach(p -> p.sendMessage(ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Go !"));
                cancel();
                startGame();
            }
        }
    };
    private final BukkitRunnable s10Timer = new BukkitRunnable() {
        int secs = 10;
        @Override
        public void run() {
            if(secs>0) {
                both.forEach(p ->
                {
                    p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Switching in " + secs + "...");
                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 1f);
                });
                secs--;
            }
            else {
                both.forEach(p -> p.sendMessage(ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Switching!"));
                cancel();
                switchPlace();
            }
        }
    };
    
    
    private void start10sTimer() {
        s10Timer.runTaskTimer(plugin, 0L, 20L);
    }
    
    
    
    private void switchPlace() {
        float fd1 = p1.getFallDistance();
        float fd2 = p2.getFallDistance();
        
        Location l1 = p1.getLocation();
        Location l2 = p2.getLocation();
        
        Vector v1 = p1.getVelocity();
        Vector v2 = p2.getVelocity();
        
        p1.teleport(l2, PlayerTeleportEvent.TeleportCause.PLUGIN);
        p1.setVelocity(v2);
        p1.setFallDistance(fd2);
        
        p2.teleport(l1, PlayerTeleportEvent.TeleportCause.PLUGIN);
        p2.setVelocity(v1);
        p2.setFallDistance(fd1);
    }
    
    public void start()
    {
        both.forEach(p -> p.sendMessage("Generating new world..."));
    
        HyperWorld world;
        WorldConfiguration worldConfiguration = WorldConfiguration.builder()
                .setName("world-" + random.nextInt(1000) + "-" + random.nextInt(1000) + "-" + random.nextInt(1000)).setType(WorldType.OVER_WORLD)
                .setWorldFeatures(WorldFeatures.NORMAL).createWorldConfiguration();
        try {
            world = Hyperverse.getApi().createWorld(worldConfiguration);
            this.world = world;
            start5stimer();
        } catch (HyperWorldCreationException e) {
            e.printStackTrace();
        }
    }
    
    private void start5stimer() {
        s5TimerTask = s5Timer.runTaskTimer(plugin, 0L, 20L);
    }
    
    private void resetForStartup(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        player.setSaturation(20);
        player.setFoodLevel(20);
        player.setHealth(20);
        player.getInventory().clear();
        player.updateInventory();
        player.setTotalExperience(0);
        player.setAbsorptionAmount(0);
        player.resetCooldown();
        player.setPlayerWeather(WeatherType.CLEAR);
    }
    
    private void startGame() {
        Location spawnL = world.getSpawn();
        if(spawnL==null)
        {
            Bukkit.broadcastMessage("Error in getting spawn location for world "+world.getDisplayName());
            clear();
            end(null);
            return;
        }
        p1.teleport(spawnL, PlayerTeleportEvent.TeleportCause.PLUGIN);
        p2.teleport(spawnL, PlayerTeleportEvent.TeleportCause.PLUGIN);
        spawnL.getWorld().setTime(0);
        spawnL.getWorld().setPVP(false);
        resetForStartup(p1);
        resetForStartup(p2);
        Bukkit.advancementIterator().forEachRemaining(adv-> {
            adv.getCriteria().forEach(crit -> {
                p1.getAdvancementProgress(adv).revokeCriteria(crit);
                p2.getAdvancementProgress(adv).revokeCriteria(crit);
            });
        });
        runTask = runTimer.runTaskTimer(plugin, 11990, 5990);
    }
    
    void end(Player winner) {
        if(winner!=null) {
            Player loser = getOther(winner);
            World w = Bukkit.getWorlds().get(0);
            Location l = w.getSpawnLocation();
            if (winner.isOnline()) {
                winner.teleport(l); // Back to spawn
                winner.sendTitle(ChatColor.DARK_GREEN + "You won against " + loser.getName() + "!", "", 20, 40, 20);
            }
            if (loser.isOnline()) {
                loser.teleport(l); // Back to spawn
                loser.sendTitle(ChatColor.DARK_RED + "You lost against " + winner.getName() + "...", "", 20, 40, 20);
            }
            Bukkit.broadcastMessage(winner.getName()+" won against "+loser.getName()+"!");
        }
            clear();
        
        if(this.world==null)
            Bukkit.broadcastMessage("World is null for pair "+p1.getName()+" - "+p2.getName()+".");
        else {
            Path p = Objects.requireNonNull(this.world.getBukkitWorld()).getWorldFolder().toPath().toAbsolutePath();
            HyperWorld.WorldUnloadResult wur = this.world.unloadWorld(false);
            Hyperverse.getApi().getWorldManager().unregisterWorld(this.world);
            Consumer<HyperWorld.WorldUnloadResult> cons = worldUnloadResult -> {
                Bukkit.broadcastMessage("Accept: "+worldUnloadResult);
                Bukkit.broadcastMessage("Desc: "+worldUnloadResult.getDescription());
            };
            this.world.deleteWorld(cons);
            try {
                deleteDirectoryStream(p);
            } catch (IOException e) {
                e.printStackTrace();
            }
    
        }

    }
    
    private void deleteDirectoryStream(Path path) throws IOException
    {
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }
}
