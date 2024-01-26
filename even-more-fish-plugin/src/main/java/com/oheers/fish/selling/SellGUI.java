package com.oheers.fish.selling;

import com.devskiller.friendly_id.FriendlyId;
import com.oheers.fish.EvenMoreFish;
import com.oheers.fish.FishUtils;
import com.oheers.fish.NbtUtils;
import com.oheers.fish.config.messages.ConfigMessage;
import com.oheers.fish.config.messages.Message;
import com.oheers.fish.database.DataManager;
import de.tr7zw.changeme.nbtapi.NBTItem;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.*;

public class SellGUI implements InventoryHolder {

    private final Player player;
    private final Inventory menu;

    public boolean modified;

    public double value;

    public boolean error;

    public int fishCount;

    public int guiSize;

    private ItemStack sellIcon, sellAllIcon, filler, errorFiller, confirmIcon, confirmSellAllIcon, noValueIcon, sellAllErrorIcon;

    public SellGUI(Player p, boolean open) {
        this.guiSize = (EvenMoreFish.mainConfig.getGUISize() + 1) * 9;
        this.player = p;
        this.modified = false;
        this.menu = Bukkit.createInventory(this, guiSize, new Message(ConfigMessage.WORTH_GUI_NAME).getRawMessage(true, true));
        setFiller();
        addFiller(filler);
        setSellItem();
        setSellAllItem();
        if (open) {
            this.player.openInventory(menu);
        }
    }

    public Player getPlayer() {
        return player;
    }

    public void setFiller() {
        // the gray glass panes at the bottom
        ItemStack fill = new ItemStack(Material.valueOf(EvenMoreFish.mainConfig.getFiller())), error = new ItemStack(Material.valueOf(EvenMoreFish.mainConfig.getFillerError()));
        ItemMeta fillMeta = fill.getItemMeta(), errMeta = error.getItemMeta();
        if (fillMeta != null) {
            fillMeta.setDisplayName(ChatColor.RESET + "");
            fill.setItemMeta(fillMeta);
            this.filler = WorthNBT.attributeDefault(fill);
        }

        if (errMeta != null) {
            errMeta.setDisplayName(ChatColor.RESET + "");
            error.setItemMeta(errMeta);
            this.errorFiller = WorthNBT.attributeDefault(error);
        }
    }

    public void addFiller(ItemStack fill) {
        for (int i = guiSize - 9; i < guiSize; i++) {
            ItemStack item = menu.getItem(i);
            if (item == null || item.isSimilar(filler) || item.isSimilar(errorFiller)) {
                menu.setItem(i, fill);
            }
        }
    }


    public void setSellAllItem() {
        ItemStack saIcon = new ItemStack(EvenMoreFish.mainConfig.getSellAllMaterial());

        ItemMeta saMeta = saIcon.getItemMeta();
        saMeta.setDisplayName(new Message(ConfigMessage.WORTH_GUI_SELL_ALL_BUTTON_NAME).getRawMessage(true, false));

        saIcon.setItemMeta(saMeta);
        glowify(saIcon);

        this.sellAllIcon = WorthNBT.attributeDefault(saIcon);
        menu.setItem(guiSize - (10 - EvenMoreFish.mainConfig.getSellAllSlot()), this.sellAllIcon);

        updateSellAllItem();
    }

    public void updateSellAllItem() {
        ItemMeta saMeta = this.sellAllIcon.getItemMeta();

        // Generates the lore, looping through each line in messages.yml lore thingy, and generating it
        Message message = new Message(ConfigMessage.WORTH_GUI_SELL_ALL_BUTTON_LORE);
        message.setSellPrice(String.valueOf(formatWorth(getTotalWorth(true))));

        saMeta.setLore(Arrays.asList(message.getRawMessage(true, true).split("\n")));

        this.sellAllIcon.setItemMeta(saMeta);
        menu.setItem(guiSize - (10 - EvenMoreFish.mainConfig.getSellAllSlot()), this.sellAllIcon);
    }

    public void setSellItem() {
        ItemStack sIcon = new ItemStack(Material.valueOf(EvenMoreFish.mainConfig.getSellItem()));

        ItemMeta sellMeta = sIcon.getItemMeta();
        sellMeta.setDisplayName(new Message(ConfigMessage.WORTH_GUI_SELL_BUTTON_NAME).getRawMessage(true, false));

        sIcon.setItemMeta(sellMeta);

        glowify(sIcon);

        this.sellIcon = WorthNBT.attributeDefault(sIcon);
        menu.setItem(guiSize - (10 - EvenMoreFish.mainConfig.getSellSlot()), this.sellIcon);

        updateSellItem();
    }

    public void updateSellItem() {
        ItemMeta sellMeta = this.sellIcon.getItemMeta();

        // Generates the lore, looping through each line in messages.yml lore thingy, and generating it
        Message message = new Message(ConfigMessage.WORTH_GUI_SELL_LORE);
        message.setSellPrice(String.valueOf(formatWorth(getTotalWorth(false))));

        sellMeta.setLore(new ArrayList<>(Arrays.asList(message.getRawMessage(true, true).split("\n"))));

        this.sellIcon.setItemMeta(sellMeta);
        menu.setItem(guiSize - (10 - EvenMoreFish.mainConfig.getSellSlot()), this.sellIcon);
    }

    /**
     * Resets the glass colour to the default one after the error glass has been used due to a value of $0 in the shop.
     * This prevents the red from hanging when the gold ingot / raw fish cod have returned.
     */
    public void resetGlassColour() {
        addFiller(this.filler);
    }

    public ItemStack getSellIcon() {
        return this.sellIcon;
    }

    public ItemStack getSellAllIcon() {
        return this.sellAllIcon;
    }

    public ItemStack getConfirmIcon() {
        return this.confirmIcon;
    }

    public ItemStack getConfirmSellAllIcon() {
        return this.confirmSellAllIcon;
    }

    public ItemStack getErrorIcon() {
        return this.noValueIcon;
    }

    public ItemStack getSellAllErrorIcon() {
        return this.sellAllErrorIcon;
    }

    public void createIcon(boolean sellAll) {
        double totalWorth = getTotalWorth(sellAll);
        if (totalWorth == 0.0) {

            ItemStack error;
            if (sellAll) error = new ItemStack(EvenMoreFish.mainConfig.getSellAllErrorMaterial());
            else error = new ItemStack(Material.valueOf(EvenMoreFish.mainConfig.getSellItemError()));

            ItemMeta errorMeta = error.getItemMeta();

            if (sellAll)
                errorMeta.setDisplayName(new Message(ConfigMessage.WORTH_GUI_NO_VAL_ALL_BUTTON_NAME).getRawMessage(true, false));
            else
                errorMeta.setDisplayName(new Message(ConfigMessage.WORTH_GUI_NO_VAL_BUTTON_NAME).getRawMessage(true, false));

            if (sellAll) {
                Message message = new Message(ConfigMessage.WORTH_GUI_SELL_BUTTON_LORE);
                message.setSellPrice(formatWorth(0.0));
                errorMeta.setLore(new ArrayList<>(Arrays.asList(message.getRawMessage(true, true).split("\n"))));
            } else {
                Message message = new Message(ConfigMessage.WORTH_GUI_NO_VAL_BUTTON_LORE);
                message.setSellPrice(formatWorth(0.0));
                errorMeta.setLore(new ArrayList<>(Arrays.asList(message.getRawMessage(true, true).split("\n"))));
            }

            error.setItemMeta(errorMeta);
            glowify(error);
            if (sellAll) this.sellAllErrorIcon = WorthNBT.attributeDefault(error);
            else this.noValueIcon = WorthNBT.attributeDefault(error);
            this.error = true;
        } else {

            ItemStack confirm;
            if (sellAll) confirm = new ItemStack(EvenMoreFish.mainConfig.getSellAllConfirmMaterial());
            else confirm = new ItemStack(Material.valueOf(EvenMoreFish.mainConfig.getSellItemConfirm()));

            ItemMeta cMeta = confirm.getItemMeta();
            if (sellAll)
                cMeta.setDisplayName(new Message(ConfigMessage.WORTH_GUI_CONFIRM_ALL_BUTTON_NAME).getRawMessage(true, false));
            else
                cMeta.setDisplayName(new Message(ConfigMessage.WORTH_GUI_CONFIRM_BUTTON_NAME).getRawMessage(true, false));
            // Generates the lore, looping through each line in messages.yml lore thingy, and generating it
            List<String> lore = new ArrayList<>();

            if (sellAll) {
                Message message = new Message(ConfigMessage.WORTH_GUI_SELL_ALL_BUTTON_LORE);
                message.setSellPrice(String.valueOf(formatWorth(getTotalWorth(true))));
                cMeta.setLore(Arrays.asList(message.getRawMessage(true, true).split("\n")));
            } else {
                Message message = new Message(ConfigMessage.WORTH_GUI_SELL_LORE);
                message.setSellPrice(String.valueOf(formatWorth(getTotalWorth(false))));

                cMeta.setLore(new ArrayList<>(Arrays.asList(message.getRawMessage(true, true).split("\n"))));
            }

            confirm.setItemMeta(cMeta);
            glowify(confirm);

            if (sellAll) this.confirmSellAllIcon = WorthNBT.attributeDefault(confirm);
            else this.confirmIcon = WorthNBT.attributeDefault(confirm);

            this.error = false;
        }
    }

    public void setIcon(boolean sellAll) {
        if (this.error) {
            if (sellAll) {
                this.menu.setItem(guiSize - (10 - EvenMoreFish.mainConfig.getSellAllSlot()), this.sellAllErrorIcon);
            } else {
                this.menu.setItem(guiSize - (10 - EvenMoreFish.mainConfig.getSellSlot()), this.noValueIcon);
            }

            this.addFiller(errorFiller);
            this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 1.0f, 0.0f);
        } else {
            if (sellAll) {
                this.menu.setItem(guiSize - (10 - EvenMoreFish.mainConfig.getSellAllSlot()), this.confirmSellAllIcon);
            } else {
                this.menu.setItem(guiSize - (10 - EvenMoreFish.mainConfig.getSellSlot()), this.confirmIcon);
            }

            this.addFiller(filler);
            this.player.playSound(this.player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.75f);
        }

    }
    public List<SoldFish> getTotalSoldFish(boolean inventory) {
        if (this.menu == null)
            return Collections.emptyList();
        
        List<SoldFish> soldFish = new ArrayList<>();
        
        if (inventory) {
            for (ItemStack item : player.getInventory().getStorageContents()) {
                // -1.0 is given when there's no worth NBT value
                SoldFish fish = getSoldFish(item);
                if(fish != null) {
                    soldFish.add(fish);
                }
            }
        } else {
            for (ItemStack item : this.menu.getContents()) {
                // -1.0 is given when there's no worth NBT value
                SoldFish fish = getSoldFish(item);
                if(fish != null) {
                    soldFish.add(fish);
                }
            }
        }
        return soldFish;
    }
    
    private @Nullable SoldFish getSoldFish(final ItemStack item) {
        if (item == null || item.getType() == Material.AIR || item.getAmount() == 0) {
            return null;
        }

        double itemValue = WorthNBT.getValue(item);
        if (itemValue == -1.0) {
            return null;
        }
        
        NBTItem nbtItem = new NBTItem(item);
        final String fishName = NbtUtils.getString(nbtItem, NbtUtils.Keys.EMF_FISH_NAME);
        final String fishRarity = NbtUtils.getString(nbtItem, NbtUtils.Keys.EMF_FISH_RARITY);
        Float floatLength = NbtUtils.getFloat(nbtItem, NbtUtils.Keys.EMF_FISH_LENGTH);
        final double fishLength = floatLength == null ? -1.0 : floatLength;
        final double fishValue = WorthNBT.getValue(item);
        
        return new SoldFish(fishName, fishRarity, item.getAmount(), fishValue * item.getAmount(), fishLength);
    }
    
    
    public double getTotalWorth(final List<SoldFish> soldFish) {
        double totalValue = 0.0d;
        int count = 0;
        for(SoldFish sold: soldFish) {
            totalValue += sold.getTotalValue();
            count += sold.getAmount();
        }
        this.value = totalValue;
        this.fishCount = count;
        
        return Math.floor(totalValue * 10) / 10;
    }

    public double getTotalWorth(boolean inventory) {
        return getTotalWorth(getTotalSoldFish(inventory));
    }

    public String formatWorth(double totalWorth) {
        if (EvenMoreFish.mainConfig.getSellType().equals("money")) {
            DecimalFormat format = new DecimalFormat(new Message(ConfigMessage.SELL_PRICE_FORMAT).getRawMessage(false, false));
            return format.format(totalWorth);
        } else {
            return (int) totalWorth + " Claim Blocks";
        }
    }

    // will drop only non-fish items if the method is called from selling, and everything if it's just a gui close
    public void close() {
        player.closeInventory();
    }

    // for each item in the menu, if it isn't a default menu item, it's dropped at the player's feet
    public void doRescue() {
        List<ItemStack> throwing = new ArrayList<>();
        for (ItemStack i : this.menu.getContents()) {
            if (i != null) {
                if (!WorthNBT.isDefault(i)) {
                    throwing.add(i);
                }
            }
        }
        FishUtils.giveItems(throwing, this.player);
    }

    public ItemStack getFiller() {
        return this.filler;
    }

    public ItemStack getErrorFiller() {
        return this.errorFiller;
    }

    public boolean getModified() {
        return this.modified;
    }

    public void setModified(boolean mod) {
        this.modified = mod;
    }

    private void glowify(ItemStack i) {

        // plops on the unbreaking 1 enchantment to make it glow
        i.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        ItemMeta meta = i.getItemMeta();

        // hides the unbreaking 1 enchantment from showing in the lore
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        i.setItemMeta(meta);
    }

    public boolean sell(boolean sellAll) {
        List<SoldFish> soldFish = getTotalSoldFish(sellAll);
        double totalWorth = getTotalWorth(soldFish);
        String sellType = EvenMoreFish.mainConfig.getSellType();
        double sellPrice = Math.floor(totalWorth * 10) / 10;

        if (sellType.equals("money")) {
            if (EvenMoreFish.econ != null) {
                EvenMoreFish.econ.depositPlayer(this.player, totalWorth);
            }
        } else if (sellType.equals("claimblocks")) {
            PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(this.player.getUniqueId());
            playerData.setBonusClaimBlocks((int) (playerData.getBonusClaimBlocks() + sellPrice));
        }

        // sending the sell message to the player

        Message message = new Message(ConfigMessage.FISH_SALE);
        message.setSellPrice(formatWorth(sellPrice));
        message.setAmount(Integer.toString(fishCount));
        message.setPlayer(this.player.toString());
        message.broadcast(player, true, true);

        this.player.playSound(this.player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.06f);

        if (sellAll) {
            for (ItemStack item : this.player.getInventory()) {
                if (FishUtils.isFish(item)) this.player.getInventory().remove(item);
            }
        } else {
            // Remove sold items
            for (int i = 0; i < guiSize - 9; i++) {
                ItemStack item = menu.getItem(i);
                if (WorthNBT.getValue(item) != -1.0) {
                    menu.setItem(i, null);
                }
            }
        }
        if (EvenMoreFish.mainConfig.databaseEnabled()) logSoldFish(player.getUniqueId(),soldFish);
        return totalWorth != 0.0;
    }
    
    private void logSoldFish(final UUID uuid, @NotNull List<SoldFish> soldFish) {
        int userId = EvenMoreFish.databaseV3.getUserID(uuid);
        final String transactionId = FriendlyId.createFriendlyId();
        final Timestamp timestamp = Timestamp.from(Instant.now());

        EvenMoreFish.databaseV3.createTransaction(transactionId, userId, timestamp);
        for(final SoldFish fish: soldFish) {
            EvenMoreFish.databaseV3.createSale(transactionId, timestamp, userId, fish.getName(),fish.getRarity(), fish.getAmount(),fish.getLength(), fish.getTotalValue());
        }
        
        double moneyEarned = getTotalWorth(soldFish);
        int fishSold = calcFishSold(soldFish);
        DataManager.getInstance().getUserReportIfExists(uuid).incrementFishSold(fishSold);
        DataManager.getInstance().getUserReportIfExists(uuid).incrementMoneyEarned(moneyEarned);
    }
    
    private int calcFishSold(@NotNull List<SoldFish> soldFish) {
        return soldFish.stream().mapToInt(SoldFish::getAmount).sum();
    }

    @Override
    public @NotNull Inventory getInventory() {
        return menu;
    }
}
