package com.sky.main;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Kit {

	
	public String menuname = "NULL";
	public ItemStack menuitem = new ItemStack(Material.BEDROCK, -1);
	public ArrayList<String> lore = new ArrayList<String>();
	public ArrayList<ItemStack> contents = new ArrayList<ItemStack>();
	
	public Kit(YamlConfiguration yml, String kit)
	{
		kit = "kits." + kit;
		System.out.println("Loading kit " + kit);
		
		String temp = yml.getString(kit + ".name");
		if(temp!=null)
		{
			this.menuname = temp.replace("&", "§");
		}
		
		if(yml.getList(kit + ".lore")!=null)
		{
			for(String string:  yml.getStringList(kit + ".lore"))
			{
				this.lore.add(string.replace("&", "§"));
			}
		}
		
		temp = yml.getString(kit + ".menuitem");
		if(temp!=null)
		{
			this.menuitem = getItem(temp);
			ItemMeta meta = this.menuitem.getItemMeta();
			meta.setDisplayName(this.menuname);
			meta.setLore(this.lore);
			this.menuitem.setItemMeta(meta);
		}
		
		if(yml.getList(kit + ".items")!=null)
		{
			for(String string: yml.getStringList(kit + ".items"))
			{
				contents.add(getItem(string));
			}
		}
	}
	
	public ItemStack getItem(String itemstring)
	{
		String[] list = itemstring.split(",");
		ItemStack stack = new ItemStack(Material.BEDROCK, -1);
		int amount = 1;
		if(list.length < 1)
		{
			return new ItemStack(Material.BEDROCK, -1);			
		}
		
		if(list.length > 1)
		{
			try
			{
				amount = Integer.parseInt(list[1]);
			}
			catch(NumberFormatException e)
			{
				//no number specified correctly, we assume they only want one.
			}
		}		
		
		//TODO: make potions work.
		
		String[] item = list[0].split(":");
		int itemid = 7;
		short metadata = 0;
		if(item.length > 1)
		{
			try
			{
				metadata = Short.parseShort(item[1]);
			}
			catch(NumberFormatException e)
			{
				//no metadata specified correctly, we assume its 0.
			}
		}
		try
		{
			itemid = Integer.parseInt(item[0]);
		}
		catch(NumberFormatException e)
		{
			//something obviously isnt filled in correctly, so we make the item bedrock to indicate
		}
		@SuppressWarnings("deprecation")
		Material mat = Material.getMaterial(itemid);
		if(mat==null)
		{
			mat = Material.BEDROCK;
		}
		stack = new ItemStack(mat, amount, metadata);
		
		//add enchantments
		if(list.length > 2)
		{
			StringBuilder ench = new StringBuilder();
			for(int i = 2; i < list.length; i++)
			{
				ench.append(list[i] + ",");
			}
			stack = enchantItem(ench.toString(), stack);
		}
		
		return stack;
	}
	
	@SuppressWarnings("deprecation")
	public ItemStack enchantItem(String enchantments, ItemStack item)
	{
		String[] Sench = enchantments.split(",");
		for(int i =0; i < Sench.length; i++)
		{
			if(Sench[i]==null)
			{
				continue;
			}
			String[] currentEnch = Sench[i].split(":");
			int id = -1;
			int level = 1;
			try
			{
				id = Integer.parseInt(currentEnch[0]);
				if(currentEnch.length > 1)
				{
					level = Integer.parseInt(currentEnch[1]);
				}
			}
			catch(NumberFormatException e)
			{
				//no correct id or level, so we continue with the next enchantment.
				continue;
			}
			item.addUnsafeEnchantment(Enchantment.getById(id), level);
		}
		return item;
	}
}
