import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class WordCounter{
    String plainText;
    public int characters;
    public int words;
    public int lines;

    public List<String> errors = new ArrayList<>();

    public WordCounter(String s){
        plainText = s;
    }

    public boolean validate(){
        Lexer lexer = new Lexer(plainText);
        List<Token> tokens = lexer.tokenize();
        if(lexer.errors.size() > 0){
            errors.add("LEXER ERROR: Invalid Character(s): ");
            errors.addAll(lexer.errors);
            return false;
        }
        this.characters = tokens.size();
        Parser parser = new Parser(tokens);

        parser.parse();
        parser.findErrors();
        if(parser.errors.size() > 0){
            errors.add("PARSER ERROR: Grammar Violation(s)");
            errors.addAll(parser.errors);

            return false;
        }
        this.words = parser.getWords();
        this.lines = parser.getLines();
        return true;
    }

}

//tokens are 
class Token{

    String type;
    String value;

    Token(String type, String value){
        this.type = type;
        this.value = value;
    }

    public String toString(){

        return "["+type+", "+value+"]";
    }

    //Tokens are equal if they are of the same "type"
    @Override
    public boolean equals(Object o){
        if(o instanceof Token){
            Token t = (Token) o;

            if(t.type.equals(this.type)){
                if(t.value.equals(this.value)){
                    return true;
                }
                else if (t.value.equals("any") || this.value.equals("any")){
                    return true;
                }
                else{
                    return false;
                }
            }
            else{
                return false;
            }
        }
        else{
            return false;
        }
    }
}

//Lexer converts from plain text to a list of tokens
class Lexer{
    String source;
    List<String> errors;

    Lexer (String data){
        this.source = data;
        errors = new ArrayList<>();
    }
    //Valid characters based on grammar
    private static char[] unit = new char[]{'A','B','C','D','E','F','G','H','I','J','K','L','M',
                            'N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
                            'a','b','c','d','e','f','g','h','i','j','k','l','m',
                            'n','o','p','q','r','s','t','u','v','w','x','y','z',
                            '0','1','2','3','4','5','6','7','8','9'};
    private static char[] separator = new char[]{':',';','-'};
    private static char dot = '.';
    private static char blank = ' ';

    private static Token createToken(char c){
        Token t;
        String value = ""+c;
        if(c == dot){
            t = new Token("dot", value);
        }
        else if(c == blank){
            t = new Token("blank", value);
        }
        else if(new String(unit).indexOf(c) > -1){
            t = new Token("unit", value);
        }
        else if (new String(separator).indexOf(c) > -1){
            t = new Token("separator", value);
        }
        else if (c == '\n'){
            t = new Token("terminal", "\\n");
        }
        else if(c == '\r'){
            t = new Token("invalid", "\\r");
        }
        else{
            //invalid character
            t = new Token("invalid", value);
        }

        return t;
    }

    public List<Token> tokenize(){

        List<Token> tokens = new ArrayList<Token>();
        
        for(int i = 0; i < source.length(); i++ ){

            Token t = createToken(source.charAt(i));
            if (t.type.equals("invalid")){
                errors.add(t.value);
            }
            else{
                tokens.add(t);
            }
        }

        return tokens;
    }
}

class Symbol{
    String type; 
    List<Object> sequence; //sequence of symbols or tokens the current symbol expands into 

    Symbol(String type, List<Object> sequence){
        this.type = type;
        this.sequence = sequence;
    }

    public String toString(){
        return "<"+type+">";
    }

    public List<Object> expand(){
        return sequence;
    }

    @Override
    public boolean equals(Object o){
        if(o instanceof Symbol){
            Symbol t = (Symbol) o;
            return(t.type.equals(this.type));
        }
        else{
            return false;
        }
    }
}

class Rule{
    String name;
    List<Object> extendsInto;

    Rule(String name){
        this.name = name;
        extendsInto = new ArrayList<Object>();
    }

    public void addExpansion(Object expansion){
        extendsInto.add(expansion);
    }
    
}

class Parser{

    static List<Object> Source; //List of tokens being parsed
    static int totalWords;
    static int totalLines;

    Rule blankRule = new Rule("blank");
    Rule unitRule = new Rule("unit");
    Rule wordRule = new Rule("word");
    Rule dotRule = new Rule("dot");
    Rule sepRule = new Rule("separator");
    Rule wordsRule = new Rule("words");
    Rule lineRule = new Rule("line");
    Rule linesRule = new Rule("lines");
    Rule fileRule = new Rule("file");

    public List<String> errors; //list of gramma
    
    Parser(List<Token> tokens){
        Source = new ArrayList<Object>();
        Source.addAll(tokens); //Source content begins by containing only the tokens
        errors = new ArrayList<>();

        //declare all rules
        List<Object> dotSeq= new ArrayList<>();
            dotSeq.add(new Token("dot", "any"));
            dotSeq.add(new Symbol("blank", null));
        dotRule.addExpansion(new Token("dot", "any"));
        dotRule.addExpansion(dotSeq);

        List<Object> blankSeq= new ArrayList<>();
            blankSeq.add(new Symbol("blank", null));
            blankSeq.add(new Token("blank", "any"));
        blankRule.addExpansion(new Token("blank", "any"));
        blankRule.addExpansion(blankSeq);

        sepRule.addExpansion(new Symbol("blank", null));
        sepRule.addExpansion(new Symbol("dot", null));
        sepRule.addExpansion(new Token("separator", "any"));

        unitRule.addExpansion(new Token("unit", "any"));

        List<Object> wordSeq= new ArrayList<>();
            wordSeq.add(new Symbol("word", null));
            wordSeq.add(new Symbol("unit", null));

        wordRule.addExpansion(new Symbol("unit", null));
        wordRule.addExpansion(wordSeq);

        List<Object> wordsSeq = new ArrayList<>();
            wordsSeq.add(new Symbol("words", null));
            wordsSeq.add(new Symbol("separator", null));
            wordsSeq.add(new Symbol("word", null));
        wordsRule.addExpansion(new Symbol("word", null));
        wordsRule.addExpansion(wordsSeq);

        List<Object> lineSeq = new ArrayList<>();
            lineSeq.add(new Symbol("words", null));
            lineSeq.add(new Token("terminal", "\\n"));

        lineRule.addExpansion(lineSeq);

        List<Object> linesSeq = new ArrayList<>();
            linesSeq.add(new Symbol ("lines", null));
            linesSeq.add(new Symbol ("line", null));
        linesRule.addExpansion(new Symbol("line", null));
        linesRule.addExpansion(linesSeq);

        fileRule.addExpansion(new Symbol("lines", null));
    }

    public void parse(){
        parse(blankRule);
        parse(dotRule);
        parse(sepRule);
        parse(unitRule);
        parse(wordRule);
        countWords();
        parse(wordsRule);
        parse(lineRule);
        countLines();
        parse(linesRule);
        parse(fileRule);
    }

    private static void parse(Rule r){
        boolean parsable = true;
        int position = r.extendsInto.size() -1 ;

        while (parsable) {
            Object sequence = r.extendsInto.get(position);
            int index;

            if(sequence instanceof ArrayList){ //The expansion is a sequence
                List<?> seqToReplace = (List<?>) sequence;
                index=Collections.indexOfSubList(Source , seqToReplace);
                if(index > -1){
                    reduceSequence(index, seqToReplace, r.name);
                    position = r.extendsInto.size() -1;
                }
                else{
                    position--;
                }
            }
            else{ //the expansion is a single object
                index = Source.indexOf(sequence);
                if(index > -1){
                    reduceSymbol(index, r.name);
                    position = r.extendsInto.size() -1;
                }
                else{
                    position--;
                }
            }
            if(position < 0){parsable = false;}
        }
    }


    private static void reduceSequence(int startIndex, List<?> seqToReplace, String replacement){
        //Save original sequence
        List<Object> ogElements = new ArrayList<>(); 
        for(int i = startIndex; i < seqToReplace.size(); i++){
            ogElements.add(Source.get(i));
        }
        //replace the expansion with the parent symbol
        Symbol s = new Symbol(replacement, ogElements);
        Source.set(startIndex, s);
        //remove the rest of the symbols in the sequence
        int endIndex = startIndex + seqToReplace.size() -1 ;
        
        for(int i = startIndex+1; i <= endIndex; i++){
            Source.remove(startIndex+1);
        }
    }

    private static void reduceSymbol(int index, String replacement){
        //save the original content 
        List<Object> ogElement = new ArrayList<>();
        ogElement.add(Source.get(index));

        //replace the source element with a new one
        Symbol s = new Symbol(replacement, ogElement);
        Source.set(index, s);
    }

   private static void countWords(){
        int words = 0;
        Symbol targetSymbol = new Symbol("word", null);
        for(int i = 0; i < Source.size(); i++){
            Object curObject = Source.get(i);
            if(curObject.equals(targetSymbol)){
                words++;
            }
        }
        totalWords = words;
   }

   private static void countLines(){
        int lines = 0;
        Symbol targetSymbol = new Symbol("line", null);
        for(int i = 0; i < Source.size(); i++){
            Object curObject = Source.get(i);
            if(curObject.equals(targetSymbol)){
                lines++;
            }
        }
        totalLines = lines;
   }

    public List<Object> getSourcce(){
        return Source;
    }

    public int getWords(){
        return totalWords;
    }

    public int getLines(){
        return totalLines;
    }

    public void findErrors(){
        int size = Source.size();
        //if the source could not be parsed to a single "file" symbol, it violates the grammar
        if(size > 1){
            for(int i = 0; i < size; i++){
                Object current = Source.get(i);
                errors.add(current.toString());
            }
        }
    }
}


