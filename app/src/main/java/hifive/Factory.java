package hifive;


// new part 4
public class Factory {
    private static Factory factory = null;

    public static Factory getInstance(){
        if (factory == null) {
            factory = new Factory();
        }

        return factory;
    }

    public Player createPlayer(String type){
        // new part 4
        if (type.equals("random")) {
            return new RandomPlayer();
        }
        else if (type.equals("basic")) {
            return new BasicPlayer();
        }
//        else if (type.equals("clever")) {
//            return new Clever();
//        }

        return new RandomPlayer();
    }
}