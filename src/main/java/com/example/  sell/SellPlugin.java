package com.example.sell;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

public class SellPlugin extends JavaPlugin implements CommandExecutor, Listener {

    private Economy econ = null;
    private File fiyatDosya;
    private YamlConfiguration fiyatYml;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        
        fiyatDosya = new File(getDataFolder(), "fiyatlar.yml");
        if (!fiyatDosya.exists()) {
            saveResource("fiyatlar.yml", false);
        }
        fiyatYml = YamlConfiguration.loadConfiguration(fiyatDosya);

        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) econ = rsp.getProvider();
        }

        if (getCommand("sat") != null) getCommand("sat").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("Sell Plugin aktif!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;
        menuAc(p);
        return true;
    }

    public void menuAc(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "§8§l» §6§lEŞYA SAT §8§l«");
        
        ItemStack cam = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta cm = cam.getItemMeta(); if (cm != null) { cm.setDisplayName(" "); cam.setItemMeta(cm); }
        for (int i = 45; i < 54; i++) inv.setItem(i, cam);

        ItemStack sat = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta sMeta = sat.getItemMeta();
        if (sMeta != null) {
            sMeta.setDisplayName("§a§lSAT VE PARAYI AL");
            sMeta.setLore(Arrays.asList("§7Envanterdeki eşyaları sunucuya sat."));
            sMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
            sMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            sat.setItemMeta(sMeta);
        }
        inv.setItem(49, sat);

        ItemStack iptal = new ItemStack(Material.BARRIER);
        ItemMeta iMeta = iptal.getItemMeta();
        if (iMeta != null) { iMeta.setDisplayName("§c§lKAPAT"); iptal.setItemMeta(iMeta); }
        inv.setItem(53, iptal);

        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.BLOCK_CHEST_OPEN, 1f, 1f);
        p.sendMessage("§eSatmak istediğin eşyaları üst kısma koy ve §aSAT §ebutonuna bas!");
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!e.getView().getTitle().equals("§8§l» §6§lEŞYA SAT §8§l«")) return;

        if (e.getClickedInventory() == null || e.getClickedInventory().equals(e.getView().getBottomInventory())) {
            return;
        }

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null) {
            e.setCancelled(true);
            return;
        }

        if (clicked.getType() == Material.BLACK_STAINED_GLASS_PANE || 
            clicked.getType() == Material.LIME_STAINED_GLASS_PANE || 
            clicked.getType() == Material.BARRIER) {
            e.setCancelled(true);
        } else {
            return; 
        }

        if (clicked.getType() == Material.BARRIER) {
            p.playSound(p.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1f, 1f);
            p.closeInventory();
        } 
        else if (clicked.getType() == Material.LIME_STAINED_GLASS_PANE) {
            esyalariSat(p, e.getView().getTopInventory());
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        if (!e.getView().getTitle().equals("§8§l» §6§lEŞYA SAT §8§l«")) return;

        Inventory inv = e.getInventory();
        for (int i = 0; i < 45; i++) { 
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                HashMap<Integer, ItemStack> kalanlar = p.getInventory().addItem(item);
                for (ItemStack kalan : kalanlar.values()) p.getWorld().dropItemNaturally(p.getLocation(), kalan);
                inv.setItem(i, null); 
            }
        }
    }

    private void esyalariSat(Player p, Inventory satisEnv) {
        double toplamPara = 0;
        int satilanEsyaSayisi = 0;
        boolean degerliEsyaVar = false;

        for (int i = 0; i < 45; i++) {
            ItemStack item = satisEnv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                String esyaIsmi = item.getType().name();
                
                double birimFiyat = fiyatYml.getDouble("fiyatlar." + esyaIsmi, 0.0);
                
                if (birimFiyat > 0) {
                    toplamPara += (birimFiyat * item.getAmount());
                    satilanEsyaSayisi += item.getAmount();
                    satisEnv.setItem(i, null);
                    degerliEsyaVar = true;
                } else {
                    p.sendMessage("§c" + esyaIsmi + " eşyasının satış fiyatı yok, iade edildi.");
                    HashMap<Integer, ItemStack> kalanlar = p.getInventory().addItem(item);
                    for (ItemStack kalan : kalanlar.values()) p.getWorld().dropItemNaturally(p.getLocation(), kalan);
                    satisEnv.setItem(i, null);
                }
            }
        }

        if (!degerliEsyaVar) {
            p.sendMessage("§cSatmak için geçerli (fiyatı olan) bir eşya koymadın!");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        if (econ != null) {
            econ.depositPlayer(p, toplamPara);
        }

        p.sendMessage("§a§l✔ §aBaşarıyla §e" + satilanEsyaSayisi + " §aadet eşya sattın!");
        p.sendMessage("§7Hesabına eklenen para: §e" + toplamPara + " TL");
        
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        p.sendTitle("§a§lSATIŞ BAŞARILI", "§7Kazandığın: §e" + toplamPara + " TL", 10, 50, 10);
        
        for (int i = 0; i < 20; i++) {
            p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation().add(0, 1, 0), 1, 0.5, 0.5, 0.5, 0);
        }

        p.closeInventory();
    }
                        }
