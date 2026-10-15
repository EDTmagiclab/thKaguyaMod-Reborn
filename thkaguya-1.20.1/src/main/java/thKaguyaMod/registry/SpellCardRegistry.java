package thKaguyaMod.registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import thKaguyaMod.entity.spellcard.THSpellCard;

/**
 * スペルカードの登録に関するクラス（1.20.1 移植版）
 * 纯数据注册表，无 Minecraft API 依赖，直接迁移。
 */
public class SpellCardRegistry
{
	public static Map<Integer, String> spellcardName = new HashMap<Integer, String>();
	public static Map<String, String> spellcardModDomain = new HashMap<String, String>();
	public static Map<String, Class<? extends THSpellCard>> spellcardClass = new HashMap<String, Class<? extends THSpellCard>>();
	public static Map<String, Integer> spellcardID = new HashMap<String, Integer>();

	public static boolean registerSpellCard(Class<? extends THSpellCard> spellcard_class, String domain, String name, int id)
	{
		if(idCheck(id))
		{
			spellcardName.put(id, name);
			spellcardModDomain.put(name, domain);
			spellcardClass.put(name, spellcard_class);
			spellcardID.put(name, id);
			return true;
		}
		return false;
	}

	private static boolean idCheck(int id)
	{
		for(String name : spellcardID.keySet())
		{
			if(spellcardID.get(name) == id)
			{
				return false;
			}
		}
		return true;
	}

	public static String getSpellCardName(int id)
	{
		return spellcardName.get(id);
	}

	public static String getSpellCardModDomain(String name)
	{
		return spellcardModDomain.get(name);
	}

	public static String getSpellCardModDomain(int id)
	{
		return spellcardModDomain.get(getSpellCardName(id));
	}

	public static int getSpellCardNumber(String name)
	{
		Integer id = spellcardID.get(name);
		return id != null ? id : -1;
	}

	public static Class<? extends THSpellCard> getSpellCardClass(String name)
	{
		return spellcardClass.get(name);
	}

	public static Class<? extends THSpellCard> getSpellCardClass(int id)
	{
		return spellcardClass.get(getSpellCardName(id));
	}

	public static int getNumberOfSpellCard()
	{
		return spellcardID.size();
	}

	public static List<String> getSpellCardList()
	{
		int length = getNumberOfSpellCard();
		String[] spellcardNames = new String[length + 1];
		int[] spellcardUsers = new int[length + 1];
		spellcardUsers[0] = 255;
		spellcardNames[0] = "";
		THSpellCard spellcard;

		int userID;
		int keyNumber = 0;
		int keyNumber2 = 0;
		boolean success = true;

		for(String name : spellcardID.keySet())
		{
			try {
				spellcard = spellcardClass.get(name).getDeclaredConstructor().newInstance();
				userID = spellcard.getOriginalUserName();
				int i = 0;
				keyNumber++;
				keyNumber2 = keyNumber;
				do
				{
					if(userID < spellcardUsers[i])
					{
						for(int j = keyNumber2; j > i; j--)
						{
							spellcardUsers[j] = spellcardUsers[j - 1];
							spellcardNames[j] = spellcardNames[j - 1];
							keyNumber2--;
						}
						spellcardUsers[i] = userID;
						spellcardNames[i] = name;
					}
					i++;
				}
				while(i < keyNumber2);
			} catch (ReflectiveOperationException e) {
				success = false;
				e.printStackTrace();
			}
		}

		if(success)
		{
			List<String> list = new ArrayList<String>();
			for(int i = 0; i < length; i++)
			{
				list.add(spellcardNames[i]);
			}
			return list;
		}
		return null;
	}

	public static int getSpellCardOriginalUser(String name)
	{
		Class<? extends THSpellCard> spellcard;
		THSpellCard useSpellCard;

		if((spellcard = getSpellCardClass(name)) != null)
		{
			try {
				useSpellCard = spellcard.getDeclaredConstructor().newInstance();
				return useSpellCard.getOriginalUserName();
			} catch (ReflectiveOperationException e) {
				e.printStackTrace();
			}
		}
		return -100;
	}

	public static List<String> sortSpellCard()
	{
		List<String> list = getSpellCardList();
		if(list == null)
		{
			return new ArrayList<String>();
		}
		List<String> newList = new ArrayList<String>();
		String array[] = new String[256];
		array[0] = list.get(0);

		for(int i = 1; i < list.size(); i++)
		{
			if(getSpellCardOriginalUser(array[i - 1]) > getSpellCardOriginalUser(list.get(i)))
			{
				int j = i;
				while(j > 0 && getSpellCardOriginalUser(array[j - 1]) > getSpellCardOriginalUser(list.get(j)))
				{
					array[j] = array[j - 1];
					j--;
				}
			}
			else
			{
				array[i] = list.get(i);
			}
		}

		for(int i = 0; i < list.size(); i++)
		{
			newList.add(array[i]);
		}
		return newList;
	}
}
