package com.sky.main;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.file.YamlConfiguration;

public class FileManager
{
	public static File getFile(String path)
	{
		File file = new File(path);
		File filepath = new File(file.getAbsolutePath() + "/..");

		if(!filepath.exists())
		{
			filepath.mkdirs();
		}

		if(!file.exists())
		{
			try
			{
				file.createNewFile();
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
		return file;
	}

	public static void saveFile(String path, YamlConfiguration YML)
	{
		File file = new File(path);
		try
		{
			YML.save(file);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
}