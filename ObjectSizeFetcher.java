import java.lang.instrument.Instrumentation;

public class ObjectSizeFetcher {
    private  Instrumentation instrumentation;

    public  void premain(String args, Instrumentation inst) {
        instrumentation = inst;
    }

    public  long getObjectSize(Object o) {
        return instrumentation.getObjectSize(o);
    }
}