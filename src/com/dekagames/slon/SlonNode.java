package com.dekagames.slon;

import java.util.LinkedHashMap;
import java.util.Vector;

/**
 *  Класс описывающий узел в структуре файла .slon. 
 *  Физически представляет собой блок в файле, заключенный в фигурные скобки. Слон файл состоит из одного корневого узла SlonNode, в котором может 
 * содержаться произвольное количество других узлов, а в них - произвольное количество своих узлов и т.д., образуя, таким образом, древовидную структуру 
 */
public class SlonNode {
    private	SlonNode			parent;
    private	Vector<SlonNode>	    	children;
    private	LinkedHashMap<String,String>	values;
    
    
    /**
     * Конструктор по умолчанию. Создает пустой узел, не содержащий ни дочерних узлов, ни пар "ключ" = "значение" 
     */
    public SlonNode(){
		children = new Vector<SlonNode>();
		values = new LinkedHashMap<String, String>();
    }

    
    /**
     * Задает родительский узел для узла. Если родительский узел равен null, значит узел является корнем дерева - 
     * верхним узлом в иерархии.
     * @param node родительский узел или null если текущий узел корневой
     */
    public void setParent(SlonNode node){
    	parent = node;
    }
   
    
    /**
     * Возвращает родительский узел. Если родительский узел равен null, значит узел является корнем дерева - 
     * верхним узлом в иерархии. 
     * @return родительский узел
     */
    public SlonNode getParent(){
    	return parent;
    }
    
    
    /**
     * Задает пару узел=значение. И узел и значение всегда задаются строками.
     * @param key узел, для которого нужно задать значение
     * @param value значение для выбранного узла
     */
    public void setKeyValue(String key, String value){
    	values.put(key, value);
    }

    
    /**
     * Задает значение типа boolean для ключа. В файл значение записывается в виде строки "true" или "false"
     * @param key узел, для которого нужно задать значение
     * @param value значение для выбранного узла 
     */
    public void setKeyValue(String key, boolean value){
		if (value) 
		    values.put(key, "true");
		else 
		    values.put(key, "false");
    }

    /**
     * Задает значение типа int для ключа. В файл значение записывается в виде строки - десятичной записи числа в кавычках
     * @param key узел, для которого нужно задать значение
     * @param value значение для выбранного узла 
     */
    public void setKeyValue(String key, int value){
    	values.put(key, Integer.toString(value));
    }

    /**
     * Задает значение типа float для ключа. В файл значение записывается в виде строки - десятичной записи числа в кавычках
     * @param key узел, для которого нужно задать значение
     * @param value значение для выбранного узла
     */
    public void setKeyValue(String key, float value){
        values.put(key, Float.toString(value));
    }


    /**
     * Возвращает значение по ключу. 
     * @param key ключ для которого нужно получить значение
     * @return значение в строковом представлении, как оно содержится в файле
     */
    public String getKeyValue(String key){
    	return values.get(key);
    }
    
    /**
     * Возвращает boooean значение по ключу. 
     * @param key ключ для которого нужно получить значение
     * @return строковое значение из файла, преобразованное в boolean
     */
    public boolean getKeyAsBoolean(String key){
    	return Boolean.parseBoolean(values.get(key));
    }

    /**
     * Возвращает int значение по ключу. 
     * @param key ключ для которого нужно получить значение
     * @return строковое значение из файла, преобразованное в int
     */
    public int getKeyAsInt(String key){
    	return Integer.parseInt(values.get(key));
    }

    /**
     * Возвращает float значение по ключу.
     * @param key ключ для которого нужно получить значение
     * @return строковое значение из файла, преобразованное в float
     */
    public float getKeyAsFloat(String key){
        return Float.parseFloat(values.get(key));
    }

    /**
     * Return the number of the key-value pairs.
     * @return the number of the key-value pairs
     */
    public int getValuesCount(){
    	return values.size();
    }
    
    /**
     * Возвращает таблицу пар ключ-значение.
     * @return LinkedHashMap<String><String> таблица с парами "ключ" = "значение" 
     */
    public LinkedHashMap<String, String> getValues(){
    	return values;
    }
    
    
    /**
     * Возвращает список всех ключей в таблице "ключ" = "значение" 
     * @return массив строк с названиями ключей
     */
    public String[] getKeys(){
		String[] ret = new String[values.size()];
		
		int i = 0;
		for (String key: values.keySet()){
		    ret[i] = key;
		    i++;
		}
		return ret;
    }
    
    
    /**
     * Return the number of child nodes.
     * @return the number of child nodes
     */
    public int getChildCount(){
    	return children.size();
    }
    
    /**
     * Возвращает дочерний узел с индексом index.
     * @param index дочерний узел под номером index или null если узла с таким номером не существует
     * @return child SlonNode
     */
    public SlonNode getChildAt(int index){
		if (index<0 || index>(children.size()-1)) return null;
		return children.elementAt(index);
    }
    
    /**
     * Возвращает первый узел со значением ключа key равным value или null если узла такого нету. Полезно при поиске узлов по определенному тегу (например "name" = "user1")
     * @param key название ключа для поиска
     * @param value требуемое значение ключа при поиске
     * @return первый из дочерних узлов, содержащий нужную пару "key" = "value"
     */
    public SlonNode getChildWithKeyValue(String key, String value){
		SlonNode tmpNode;
    	SlonNode ret = null;
		// переберем все дочерние узлы - ищем key=value
		for (int i=0; i<getChildCount(); i++) {
			tmpNode = getChildAt(i);
			if (tmpNode.getKeyValue(key).equals(value)) {
				ret = tmpNode;
				break;
			}
		}
		return ret;
    }
    

  
    /**
     * Добавляет узел к списку узлов.
     * @param node добавляемый узел
     */
    public void addChild(SlonNode node){
		if (node!=null){
		    node.setParent(this);
		    children.add(node);
		}
    }
    
    
    
    
}
