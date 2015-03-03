package com.dekagames.slon;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

// состояния конечного автомата
enum State{
    NULL,	    // начало документа
    END,	    // конец документа
    NODE,	    // начало нового узла - только зашел в ноду - ищем ключи или другие ноды	
    KEY_BEGIN,	    // начали читать новый ключ
    KEY_END,	    // прочитали ключ ждем равно 
    VALUE_WAIT,	    // прочитали равно - ждем значение в кавычках
    VALUE_BEGIN,    // читаем значение
    COMMENT
}
/**
 * Класс, предназначен для работы с файлами формата slon. Файл slon - что-то среднее между XML и INI.
 * Типичный файл выглядит примерно так:
 *<p>
 * # comment<br>
 * {<br>
 * key1 = "value1"<br>
 * key2 = "value2"    #comment<br>
 *	{<br>
 *	key1 = "value1"<br>
 *	key2 = "value2"<br>
 *	}<br>
 * }<br>
 * </p>
 * Файл состоит из блоков (узлов) в фигурных скобках: {  }, обязательно наличие ОДНОГО корневого блока для всего файла.
 * Каждый блок может состоять из неограниченного количества вложенных блоков и пар ключ = значение. Все значения
 * заключаются в кавычки и по сути я вляются строковыми переменными. Вложенные блоки и пары ключ=значение могут чередоваться в произвольном порядке.
 * 
 * @author Deka
 */
public class Slon {
    /// выравнивание блоков в файле при сохранении
    private static final String IDENT_BLOCK = "    ";
    
    // пустые, ничего не значащие символы - пробел, табуляция, перевод строки и возврат каретки
    private static final String	NULL_SYMBOLS = " \t\n\r";	
    // символы, из которых состоят идентификаторы - ключи
    private static final String	ID_SYMBOLS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_0123456789-";		
    
    private	int		lineNumber;		// номер строки текущей
    
    private	int		    symbol;			// текущий индекс и символ по этому индексу
    private	State		state;			// текущее состояние автомата
    private	State		prev_state;		// предыдущее состояние автомата - после выхода из коммента
    private	SlonNode	root;			// корневой узел DOM 
    private	SlonNode	curr_node;		// текущий узел в котором парсится файл

    private	StringBuilder	curr_key;		// имя текущего ключа
    private	StringBuilder	curr_value;		// значение текущего ключа
    
    // настройки сохранения
    public	boolean		valuesInLine;		// пары ключ-значение будут записаны в одну строку
    public 	boolean		saveMinimal;		// при сохранении будут выкинуты все пустые символы - пробелы, переводы строк и т.д.
    
    
    /**
     * Конструктор по умолчанию. Создает пустую структуру файла. После этого ОБЯЗАТЕЛЬНО необходимо добавить 
     * в файл корневой узел методом
     */
    public Slon(){
	    state = State.NULL;
	    valuesInLine = true;
    }
    
    /**
     * Загружает .slon файл в память и возвращает корень загруженного дерева
     * @param path - полный путь к файлу с расширением
     * @throws SlonException  
     */
    public SlonNode load(String path) throws SlonException{
		File f = new File(path);
		load(f);
		return root;
    }

    
    /**
     * Загружает .slon файл в память и возвращает корень загруженного дерева
     * @param f - дескриптор слон-файла. 
     * @throws SlonException  
     */
    public SlonNode load(File f) throws SlonException{
		try {
		    parse(new FileReader(f));
		} catch (IOException ex) {
		    Logger.getLogger(Slon.class.getName()).log(Level.SEVERE, null, ex);
		}
		return root;
    }

    /**
     * Загружает .slon файл в память и возвращает корень загруженного дерева
     * @param is - InputStream, читающий файл с устройства.
     * @throws SlonException
     */
    public SlonNode load(InputStream is) throws SlonException{
        try {
            InputStreamReader isr = new InputStreamReader(is);
            parse(isr);
        } catch (IOException ex) {
            Logger.getLogger(Slon.class.getName()).log(Level.SEVERE, null, ex);
        }
        return root;
    }


    /**
     * Загружает .slon файл в память и возвращает корень загруженного дерева
     * @param r - Reader, читающий файл с устройства. 
     * @throws SlonException  
     */
    public SlonNode load(Reader r) throws SlonException{
	try {
	    parse(r);
	} catch (IOException ex) {
	    Logger.getLogger(Slon.class.getName()).log(Level.SEVERE, null, ex);
	}
	return root;
    }

    
    
    /**
     * Сохраняет текущее дерево в файл формата .slon на диске.
     * @param path - полный путь к файлу, включая имя и расширение
     */
    public void save(String path) {
        File f = new File(path);
        FileWriter wr;
        try {
            wr = new FileWriter(f);
            save(wr);
            wr.close();
        } catch (IOException ex) {
            Logger.getLogger(Slon.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Сохраняет текущее дерево в файл формата .slon
     * @param writer - Writer, записывающий файл на устройство
     */
    public void save(Writer writer){
	    try {
	        save_node(root,writer,0);
	        writer.close();
	    } catch (IOException ex) {
	        Logger.getLogger(Slon.class.getName()).log(Level.SEVERE, null, ex);
	    }
    }
    
    // запишем один нод
    private void save_node(SlonNode node, Writer wr, int level) throws IOException{
		StringBuilder ident = new StringBuilder();
		String[] keys;
		String value;
		for (int i=0; i<level; i++) ident.append(IDENT_BLOCK);
	
		
		// откроем узел
		if (!saveMinimal) wr.write(ident.toString());
		wr.write("{"); 
		if (!saveMinimal) wr.write("\n");
		
		// запишем пары ключ-значение
		keys = node.getKeys();
		// запишем отступ первой пары
		if (!saveMinimal) wr.write(ident.toString());
		// запишем сами пары
		for (String key: keys){
		    value = node.getKeyValue(key);
		    // если значения нет - то это пустая строка
		    if (value == null) value = "";
		    // запишем в файл
		    wr.write(key+"=\""+value+"\"");
			if (!saveMinimal) wr.write(" ");	// пробел между значениями
		    
		    // если значения не в строку, то запишем перенос строки и отступ
		    if (!valuesInLine && !saveMinimal) wr.write("\n"+ident.toString());
		}
		if (!saveMinimal) wr.write("\n");
		
		// рекурсивно запишем дочерние узлы
		for (int i=0; i<node.getChildCount(); i++){
		    save_node(node.getChildAt(i),wr, level+1);
		}
		
		// закроем узел
		if (!saveMinimal) wr.write(ident.toString());
		wr.write("}");
		if (!saveMinimal) wr.write("\n");
    }
    
    /**
     * Возвращает корневой узел документа. Для работы с файлом необходимо получить корневой узел.
     */
    public SlonNode getRoot(){
	    return root;
    }

    /**
     * Задаем корневой узел дерева. Он всегда единственный, поэтому задании другого корневого узла фактически меняется содержимое всего файла.
     * @param rootNode 
     */
    public void setRoot(SlonNode rootNode){
	    root = rootNode;
    }
    
    
    
    // разбор файла
    private void parse(Reader reader) throws IOException, SlonException{
	
        lineNumber = 1;

        while (true){
            symbol = reader.read();
            if (symbol<0){
                state = State.END;
                return;
            }

            // символ комментария в кавычках допустим
            if (symbol == '#' && state != State.COMMENT && state != State.VALUE_BEGIN){
                prev_state = state;
                state = State.COMMENT;
            }

            if (symbol == '\n')
                lineNumber++;

            switch (state){
                case NULL:	    	process_null_state(symbol);			break;
                case NODE:	    	process_node_state(symbol);			break;
                case KEY_BEGIN:	    process_key_begin_state(symbol);    break;
                case KEY_END:	    process_key_end_state(symbol);		break;
                case VALUE_WAIT:    process_value_wait_state(symbol);	break;
                case VALUE_BEGIN:   process_value_begin_state(symbol);	break;
                case COMMENT:	    process_comment_state(symbol);		break;
            }
        }
    }
    
    // добавляем новую ноду
    private void open_node(){
        if (curr_node == null){	// самое начало файла - корневой узел
            root = new SlonNode();
            curr_node = root;
        }
        else {			// середина файла - вложенный узел
            SlonNode node = new SlonNode();
            node.setParent(curr_node);
            curr_node = node;
        }
    }
    
    // закырываем текущую ноду и переходим на уровень вверх
    private void close_node(){
        SlonNode node = curr_node;
        curr_node = curr_node.getParent();
        if (curr_node != null){
            curr_node.addChild(node);
        }
    }
    
    // самое начало парсинга - пропускаем все символы до первой скообки - корневого элемента 
    private void process_null_state(int elem){
        if (elem == '{') {
            state = State.NODE;
            open_node();
        }
    }
    
    // началась нода - ищем ключи или дочерние ноды
    private void process_node_state(int elem) throws SlonException{
        if (NULL_SYMBOLS.indexOf(elem)>=0) return;	    // пропускаем все пустые символы
        if (ID_SYMBOLS.indexOf(elem)>=0){		    // нашли первый символ ключа
            curr_key = new StringBuilder();
            curr_key.append((char)elem);
            state = State.KEY_BEGIN;
            return;
        }
        if (elem == '{') {
            state = State.NODE;
            open_node();
            return;
        }

        if (elem == '}'){
            state = State.NODE;
            close_node();
            return;
        }

        throw new SlonException("Invalid character. Key or Node expected in line "+Integer.toString(lineNumber));
    }
    
    // читаем ключ - он уже начался и первый символ в нем уже записан если встречаем 
    // недопусимый символ то Exception, если пробел или = то заканчиваем ввод имени 
    private void process_key_begin_state(int elem) throws SlonException{
        if (ID_SYMBOLS.indexOf(elem)>=0){
            curr_key.append((char)elem);
            return;
        }
        if (NULL_SYMBOLS.indexOf(elem)>=0){
            state = State.KEY_END;
            return;
        }

        if (elem == '='){
            state = State.VALUE_WAIT;
            return;
        }
        throw new SlonException("Invalid character in Key name in line "+Integer.toString(lineNumber));
    }
    
    // закончили читать ключ - ждем символа "=" и ничего больше
    private void process_key_end_state(int elem) throws SlonException{
        if (NULL_SYMBOLS.indexOf(elem)>=0) return;

        if (elem == '='){
            state = State.VALUE_WAIT;
            return;
        }

        throw new SlonException("Value assignment expected in line "+Integer.toString(lineNumber));
    }
    
    // ждем начала значения - только кавычка 
    private void process_value_wait_state(int elem) throws SlonException{
        if (NULL_SYMBOLS.indexOf(elem)>=0) return;
        if (elem == '"'){
            curr_value = new StringBuilder();
            state = State.VALUE_BEGIN;
            return;
        }
        throw new SlonException("Quote expected in line "+Integer.toString(lineNumber));
    }
    
    // читаем значение ключа - все до кавычки, пофиг что
    private void process_value_begin_state(int elem) {
        if (elem == '"'){
            state = State.NODE;	    // возвращаемся в чтение нода
            curr_node.setKeyValue(curr_key.toString(), curr_value.toString());
            return;
        }
        curr_value.append((char)elem);
    }
    
    // читаем комментарий пофиг что до перевода строки
    private void process_comment_state(int elem) {
	    if (elem == '\n')    state = prev_state;	    // возвращаемся в предыдущее состояние
    }
}
