package droidsafe.analyses.infoflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.tuple.ImmutablePair;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import soot.Local;
import soot.SootField;
import soot.SootMethod;
import soot.jimple.ParameterRef;
import soot.jimple.spark.pag.AllocNode;
import soot.jimple.toolkits.callgraph.Edge;

public class States {
    private final DefaultHashMap<Context, FrameHeapStatics> contextToFrameHeapStatics;

    States() {
        contextToFrameHeapStatics = new DefaultHashMap<Context, FrameHeapStatics>(FrameHeapStatics.EMPTY);
    }

    States merge(States that) {
        States states = new States();
        for (Map.Entry<Context, FrameHeapStatics> contextFrameHeapStatics : this.contextToFrameHeapStatics.entrySet()) {
            Context context = contextFrameHeapStatics.getKey();
            FrameHeapStatics frameHeapStatics = contextFrameHeapStatics.getValue();
            if (that.contextToFrameHeapStatics.containsKey(context)) {
                states.contextToFrameHeapStatics.put(context, frameHeapStatics.merge(that.contextToFrameHeapStatics.get(context)));
            } else {
                states.contextToFrameHeapStatics.put(context, frameHeapStatics);
            }
        }
        for (Map.Entry<Context, FrameHeapStatics> contextFrameHeapStatics : that.contextToFrameHeapStatics.entrySet()) {
            Context context = contextFrameHeapStatics.getKey();
            if (!this.contextToFrameHeapStatics.containsKey(context)) {
                states.contextToFrameHeapStatics.put(context, contextFrameHeapStatics.getValue());
            }
        }
        return states;
    }

    FrameHeapStatics put(Context context, FrameHeapStatics frameHeapStatics) {
        if (frameHeapStatics != null && !frameHeapStatics.isEmpty()) {
            return contextToFrameHeapStatics.put(context, frameHeapStatics);
        } else {
            return contextToFrameHeapStatics.remove(context);
        }
    }

    FrameHeapStatics get(Context context) {
        return contextToFrameHeapStatics.get(context);
    }

    Set<Map.Entry<Context, FrameHeapStatics>> entrySet() {
        return contextToFrameHeapStatics.entrySet();
    }

    Set<Context> keySet() {
        return contextToFrameHeapStatics.keySet();
    }

    Collection<FrameHeapStatics> values() {
        return contextToFrameHeapStatics.values();
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (!(that instanceof States)) {
            return false;
        }
        States states = (States)that;

        return this.contextToFrameHeapStatics.equals(states.contextToFrameHeapStatics);
    }

    @Override
    public String toString() {
        StringBuffer str = new StringBuffer();
        str.append('{');
        for (Map.Entry<Context, FrameHeapStatics> contextFrameHeapStatics : this.contextToFrameHeapStatics.entrySet()) {
            str.append(contextFrameHeapStatics.getKey().toString());
            str.append("=\\l");
            str.append(contextFrameHeapStatics.getValue().toString());
            str.append("\\l");
        }
        int length = str.length();
        if (length > 1) {
            str.setLength(length - 2);
        }
        str.append('}');
        return str.toString();
    }

    public States minus(States that) {
        States states = new States();
        for (Map.Entry<Context, FrameHeapStatics> contextFrameHeapStatics : this.contextToFrameHeapStatics.entrySet()) {
            Context context = contextFrameHeapStatics.getKey();
            FrameHeapStatics frameHeapStatics = contextFrameHeapStatics.getValue();
            states.put(context, frameHeapStatics.minus(that.get(context)));
        }
        return states;
    }
}

class Context {
    private static final HashMap<ImmutablePair<Edge, Edge>, Context> cache = new HashMap<ImmutablePair<Edge, Edge>, Context>();

    Edge entryEdge;
    Edge callEdge;

    private Context(Edge entryEdge, Edge callEdge) {
        assert entryEdge != null && callEdge != null;
        this.entryEdge = entryEdge;
        this.callEdge = callEdge;
    }

    static Context v(Edge entryEdge, Edge callEdge) {
        ImmutablePair<Edge, Edge> key = ImmutablePair.of(entryEdge, callEdge);
        Context context = cache.get(key);
        if (context == null) {
            context = new Context(entryEdge, callEdge);
            cache.put(key, context);
        }
        return context;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (!(that instanceof Context)) {
            return false;
        }
        Context context = (Context)that;

        return this.callEdge.equals(context.callEdge) && this.entryEdge.equals(context.entryEdge);
    }

    @Override
    public int hashCode() {
        return callEdge.hashCode();
    }

    @Override
    public String toString() {
        return "(" + entryEdge + ", " + callEdge + ")";
    }
}

class FrameHeapStatics {
    static final FrameHeapStatics EMPTY = new FrameHeapStatics();

    Frame frame;
    Heap heap;
    Statics statics;

    FrameHeapStatics() {
        this.frame = new Frame();
        this.heap = new Heap();
        this.statics = new Statics();
    }

    FrameHeapStatics(Frame frame, Heap heap, Statics statics) {
        assert frame != null && heap != null && statics != null;
        this.frame = frame;
        this.heap = heap;
        this.statics = statics;
    }

    FrameHeapStatics(FrameHeapStatics that) {
        this.frame = new Frame(that.frame);
        this.heap = new Heap(that.heap);
        this.statics = new Statics(that.statics);
    }

    FrameHeapStatics merge(FrameHeapStatics that) {
        return new FrameHeapStatics(this.frame.merge(that.frame), this.heap.merge(that.heap), this.statics.merge(that.statics));
    }

    boolean isEmpty() {
        return frame.isEmpty() && heap.isEmpty() && statics.isEmpty();
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (!(that instanceof FrameHeapStatics)) {
            return false;
        }
        FrameHeapStatics frameHeapStatics = (FrameHeapStatics)that;

        return this.frame.equals(frameHeapStatics.frame) && this.heap.equals(frameHeapStatics.heap) && this.statics.equals(frameHeapStatics.statics);
    }

    @Override
    public String toString() {
        return "(" + frame + ",\\l " + heap + ",\\l " + statics + ")";
    }

    public FrameHeapStatics minus(FrameHeapStatics that) {
        return new FrameHeapStatics(this.frame.minus(that.frame), this.heap.minus(that.heap), this.statics.minus(that.statics));
    }
}

class Frame {
    static final Frame EMPTY = new Frame();

    DefaultHashMap<MethodLocal, ImmutableList<MyValue>> locals;
    DefaultHashMap<SootMethod, ImmutableList<MyValue>> thiz;
    DefaultHashMap<MethodMyParameterRef, ImmutableList<MyValue>> params;

    Frame() {
        locals = new DefaultHashMap<MethodLocal, ImmutableList<MyValue>>(ImmutableList.<MyValue>of());
        thiz = new DefaultHashMap<SootMethod, ImmutableList<MyValue>>(ImmutableList.<MyValue>of());
        params = new DefaultHashMap<MethodMyParameterRef, ImmutableList<MyValue>>(ImmutableList.<MyValue>of());
    }

    Frame(Frame that, DefaultHashMap<SootMethod, ImmutableList<MyValue>> thiz, DefaultHashMap<MethodMyParameterRef, ImmutableList<MyValue>> params) {
        assert params != null;
        this.locals = new DefaultHashMap<MethodLocal, ImmutableList<MyValue>>(that.locals);
        this.thiz = thiz;
        this.params = params;
    }

    Frame(DefaultHashMap<MethodLocal, ImmutableList<MyValue>> locals, DefaultHashMap<SootMethod, ImmutableList<MyValue>> thiz, DefaultHashMap<MethodMyParameterRef, ImmutableList<MyValue>> params) {
        assert locals != null && params != null;
        this.locals = locals;
        this.thiz = thiz;
        this.params = params;
    }

    Frame(Frame that) {
        this.locals = new DefaultHashMap<MethodLocal, ImmutableList<MyValue>>(that.locals);
        this.thiz = new DefaultHashMap<SootMethod, ImmutableList<MyValue>>(that.thiz);
        this.params = new DefaultHashMap<MethodMyParameterRef, ImmutableList<MyValue>>(that.params);
    }

    Frame merge(Frame that) {
        Frame frame = new Frame();

        for (Map.Entry<MethodLocal, ImmutableList<MyValue>> localValues : this.locals.entrySet()) {
            MethodLocal local = localValues.getKey();
            ImmutableList<MyValue> values = localValues.getValue();
            if (that.locals.containsKey(local)) {
                Set<MyValue> vals = new HashSet<MyValue>(values);
                vals.addAll(that.locals.get(local));
                frame.locals.put(local, ImmutableList.copyOf(vals));
            } else {
                frame.locals.put(local, values);
            }
        }
        for (Map.Entry<MethodLocal, ImmutableList<MyValue>> localValues : that.locals.entrySet()) {
            MethodLocal local = localValues.getKey();
            if (!this.locals.containsKey(local)) {
                frame.locals.put(local, localValues.getValue());
            }
        }

        for (Map.Entry<SootMethod, ImmutableList<MyValue>> methodValues : this.thiz.entrySet()) {
            SootMethod method = methodValues.getKey();
            ImmutableList<MyValue> values = methodValues.getValue();
            if (that.thiz.containsKey(method)) {
                Set<MyValue> vals = new HashSet<MyValue>(values);
                vals.addAll(that.thiz.get(method));
                frame.thiz.put(method, ImmutableList.copyOf(vals));
            } else {
                frame.thiz.put(method, values);
            }
        }
        for (Map.Entry<SootMethod, ImmutableList<MyValue>> methodValues : that.thiz.entrySet()) {
            SootMethod method = methodValues.getKey();
            if (!this.thiz.containsKey(method)) {
                frame.thiz.put(method, methodValues.getValue());
            }
        }

        for (Map.Entry<MethodMyParameterRef, ImmutableList<MyValue>> parameterRefValues : this.params.entrySet()) {
            ImmutableList<MyValue> values = parameterRefValues.getValue();
            MethodMyParameterRef parameterRef = parameterRefValues.getKey();
            if (that.params.containsKey(parameterRef)) {
                Set<MyValue> vals = new HashSet<MyValue>(values);
                vals.addAll(that.params.get(parameterRef));
                frame.params.put(parameterRef, ImmutableList.copyOf(vals));
            } else {
                frame.params.put(parameterRef, values);
            }
        }
        for (Map.Entry<MethodMyParameterRef, ImmutableList<MyValue>> parameterRefValues : that.params.entrySet()) {
            MethodMyParameterRef parameterRef = parameterRefValues.getKey();
            if (!this.params.containsKey(parameterRef)) {
                frame.params.put(parameterRef, parameterRefValues.getValue());
            }
        }

        return frame;
    }

    ImmutableList<MyValue> putS(MethodLocal local, Set<MyValue> values) {
        return putS(local, ImmutableList.copyOf(values));
    }

    ImmutableList<MyValue> putS(MethodLocal local, ImmutableList<MyValue> values) {
        if (values != null && !values.isEmpty()) {
            return locals.put(local, values);
        } else {
            return locals.remove(local);
        }
    }

    ImmutableList<MyValue> putW(MethodLocal local, Set<MyValue> values) {
        return putW(local, ImmutableList.copyOf(values));
    }

    // weakly update
    ImmutableList<MyValue> putW(MethodLocal local, List<MyValue> values) {
        ImmutableList<MyValue> valuesOld = locals.get(local);
        if (values != null && !values.isEmpty()) {
            Set<MyValue> valuesNew = new HashSet<MyValue>(valuesOld);
            valuesNew.addAll(values);
            return locals.put(local, ImmutableList.copyOf(valuesNew));
        } else {
            return valuesOld;
        }
    }

    ImmutableList<MyValue> get(MethodLocal local) {
        return locals.get(local);
    }

    ImmutableList<MyValue> remove(MethodLocal local) {
        return locals.remove(local);
    }

    ImmutableList<MyValue> putS(SootMethod method, ImmutableList<MyValue> values) {
        if (values != null && !values.isEmpty()) {
            return thiz.put(method, values);
        } else {
            return thiz.remove(method);
        }
    }

    ImmutableList<MyValue> putS(MethodMyParameterRef parameterRef, Set<MyValue> values) {
        return putS(parameterRef, ImmutableList.copyOf(values));
    }

    // weakly update
    ImmutableList<MyValue> putW(SootMethod method, List<MyValue> values) {
        ImmutableList<MyValue> valuesOld = thiz.get(method);
        if (values != null && !values.isEmpty()) {
            Set<MyValue> valuesNew = new HashSet<MyValue>(valuesOld);
            valuesNew.addAll(values);
            return thiz.put(method, ImmutableList.copyOf(valuesNew));
        } else {
            return valuesOld;
        }
    }

    ImmutableList<MyValue> get(SootMethod method) {
        return thiz.get(method);
    }

    ImmutableList<MyValue> putS(MethodMyParameterRef parameterRef, ImmutableList<MyValue> values) {
        if (values != null && !values.isEmpty()) {
            return params.put(parameterRef, values);
        } else {
            return params.remove(parameterRef);
        }
    }

    // weakly update
    ImmutableList<MyValue> putW(MethodMyParameterRef parameterRef, List<MyValue> values) {
        ImmutableList<MyValue> valuesOld = params.get(parameterRef);
        if (values != null && !values.isEmpty()) {
            Set<MyValue> valuesNew = new HashSet<MyValue>(valuesOld);
            valuesNew.addAll(values);
            return params.put(parameterRef, ImmutableList.copyOf(valuesNew));
        } else {
            return valuesOld;
        }
    }

    ImmutableList<MyValue> get(MethodMyParameterRef parameterRef) {
        return params.get(parameterRef);
    }

    Set<Address> roots() {
        Set<Address> roots = new HashSet<Address>();
        for (ImmutableList<MyValue> values : locals.values()) {
            for (MyValue value : values) {
                if (value instanceof Address) {
                    roots.add((Address)value);
                }
            }
        }
        for (ImmutableList<MyValue> values : thiz.values()) {
            for (MyValue value : values) {
                if (value instanceof Address) {
                    roots.add((Address)value);
                }
            }
        }
        for (ImmutableList<MyValue> values : params.values()) {
            for (MyValue value : values) {
                if (value instanceof Address) {
                    roots.add((Address)value);
                }
            }
        }
        return roots;
    }

    boolean isEmpty() {
        return locals.isEmpty() && thiz.isEmpty() && params.isEmpty();
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (!(that instanceof Frame)) {
            return false;
        }
        Frame frame = (Frame)that;

        if (this.locals.size() != frame.locals.size()) {
            return false;
        }
        if (this.thiz.size() != frame.thiz.size()) {
            return false;
        }
        if (this.params.size() != frame.params.size()) {
            return false;
        }
        try {
            for (Map.Entry<MethodLocal, ImmutableList<MyValue>> localValues : this.locals.entrySet()) {
                if (!(ImmutableSet.copyOf(localValues.getValue()).equals(ImmutableSet.copyOf(frame.locals.get(localValues.getKey()))))) {
                    return false;
                }
            }
            for (Map.Entry<SootMethod, ImmutableList<MyValue>> methodValues : this.thiz.entrySet()) {
                if (!(ImmutableSet.copyOf(methodValues.getValue()).equals(ImmutableSet.copyOf(frame.thiz.get(methodValues.getKey()))))) {
                    return false;
                }
            }
            for (Map.Entry<MethodMyParameterRef, ImmutableList<MyValue>> paramValues : this.params.entrySet()) {
                if (!(ImmutableSet.copyOf(paramValues.getValue()).equals(ImmutableSet.copyOf(frame.params.get(paramValues.getKey()))))) {
                    return false;
                }
            }
        } catch (ClassCastException unused) {
            return false;
        } catch (NullPointerException unused) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        Comparator<MethodLocal> localComparator = new Comparator<MethodLocal>() {
            @Override
            public int compare(MethodLocal local1, MethodLocal local2) {
                int method = local1.method.getSignature().compareTo(local2.method.getSignature());
                if (method != 0) {
                    return method;
                } else {
                    return local1.local.getName().compareTo(local2.local.getName());
                }
            }
        };
        TreeMap<MethodLocal, ImmutableList<MyValue>> sortedLocals = new TreeMap<MethodLocal, ImmutableList<MyValue>>(localComparator);
        sortedLocals.putAll(locals);
        Comparator<SootMethod> thisComparator = new Comparator<SootMethod>() {
            @Override
            public int compare(SootMethod method1, SootMethod method2) {
                return method1.getSignature().compareTo(method2.getSignature());
            }
        };
        TreeMap<SootMethod, ImmutableList<MyValue>> sortedThese = new TreeMap<SootMethod, ImmutableList<MyValue>>(thisComparator);
        sortedThese.putAll(thiz);
        Comparator<MethodMyParameterRef> paramComparator = new Comparator<MethodMyParameterRef>() {
            @Override
            public int compare(MethodMyParameterRef param1, MethodMyParameterRef param2) {
                int method = param1.method.getSignature().compareTo(param2.method.getSignature());
                if (method != 0) {
                    return method;
                } else {
                    return param1.param.getIndex() - param2.param.getIndex();
                }
            }
        };
        TreeMap<MethodMyParameterRef, ImmutableList<MyValue>> sortedParams = new TreeMap<MethodMyParameterRef, ImmutableList<MyValue>>(paramComparator);
        sortedParams.putAll(params);
        return "(" + sortedLocals + ", " + sortedThese + ", " + sortedParams + ")";
    }

    public Frame minus(Frame that) {
        Frame frame = new Frame();
        for (Map.Entry<MethodLocal, ImmutableList<MyValue>> localValues : this.locals.entrySet()) {
            MethodLocal local = localValues.getKey();
            Set<MyValue> values = new HashSet<MyValue>(localValues.getValue());
            values.removeAll(that.get(local));
            frame.putS(local, ImmutableList.<MyValue>copyOf(values));
        }
        for (Map.Entry<SootMethod, ImmutableList<MyValue>> methodValues : this.thiz.entrySet()) {
            SootMethod method = methodValues.getKey();
            Set<MyValue> values = new HashSet<MyValue>(methodValues.getValue());
            values.removeAll(that.get(method));
            frame.putS(method, ImmutableList.<MyValue>copyOf(values));
        }
        for (Map.Entry<MethodMyParameterRef, ImmutableList<MyValue>> paramValues : this.params.entrySet()) {
            MethodMyParameterRef param = paramValues.getKey();
            Set<MyValue> values = new HashSet<MyValue>(paramValues.getValue());
            values.removeAll(that.get(param));
            frame.putS(param, ImmutableList.<MyValue>copyOf(values));
        }
        return frame;
    }
}

class MethodLocal {
    private static final HashMap<ImmutablePair<SootMethod, Local>, MethodLocal> cache = new HashMap<ImmutablePair<SootMethod, Local>, MethodLocal>();

    SootMethod method;
    Local local;

    private MethodLocal(SootMethod method, Local local) {
        assert method != null && local != null;
        this.method = method;
        this.local = local;
    }

    static MethodLocal v(SootMethod method, Local local) {
        assert method != null && local != null;
        ImmutablePair<SootMethod, Local> key = ImmutablePair.of(method, local);
        MethodLocal methodLocal = cache.get(key);
        if (methodLocal == null) {
            methodLocal = new MethodLocal(method, local);
            cache.put(key, methodLocal);
        }
        return methodLocal;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (!(that instanceof MethodLocal)) {
            return false;
        }
        MethodLocal methodLocal = (MethodLocal)that;

        return this.method.equals(methodLocal.method) && this.local.equivTo(methodLocal.local);
    }

    @Override
    public int hashCode() {
        return method.equivHashCode();
    }

    @Override
    public String toString() {
        return local.toString();
    }
}

class MethodMyParameterRef {
    private static final HashMap<ImmutablePair<SootMethod, MyParameterRef>, MethodMyParameterRef> cache = new HashMap<ImmutablePair<SootMethod, MyParameterRef>, MethodMyParameterRef>();

    SootMethod method;
    MyParameterRef param;

    private MethodMyParameterRef(SootMethod method, MyParameterRef param) {
        assert method != null && param != null;
        this.method = method;
        this.param = param;
    }

    static MethodMyParameterRef v(SootMethod method, ParameterRef param) {
        assert method != null && param != null;
        MyParameterRef p = new MyParameterRef(param);
        ImmutablePair<SootMethod, MyParameterRef> key = ImmutablePair.of(method, p);
        MethodMyParameterRef methodParam = cache.get(key);
        if (methodParam == null) {
            methodParam = new MethodMyParameterRef(method, p);
            cache.put(key, methodParam);
        }
        return methodParam;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (!(that instanceof MethodMyParameterRef)) {
            return false;
        }
        MethodMyParameterRef methodParam = (MethodMyParameterRef)that;

        return this.method.equals(methodParam.method) && this.param.equivTo(methodParam.param);
    }

    @Override
    public int hashCode() {
        return method.equivHashCode();
    }

    @Override
    public String toString() {
        return param.toString();
    }
}

@SuppressWarnings("serial")
class MyParameterRef extends ParameterRef {
    public MyParameterRef(ParameterRef param) {
        super(param.getType(), param.getIndex());
        assert !(param instanceof MyParameterRef);
    }

    @Override
    public boolean equals(Object that) {
        return equivTo(that);
    }

    @Override
    public int hashCode() {
        return equivHashCode();
    }
}

class Heap {
    Instances instances;
    Arrays arrays;

    Heap() {
        instances = new Instances();
        arrays = new Arrays();
    }

    Heap(Instances instances, Arrays arrays) {
        assert instances != null && arrays != null;
        this.instances = instances;
        this.arrays = arrays;
    }

    Heap(Heap that, Arrays arrays) {
        assert arrays != null;
        this.instances = new Instances(that.instances);
        this.arrays = arrays;
    }

    Heap(Heap that) {
        this.instances = new Instances(that.instances);
        this.arrays = new Arrays(that.arrays);
    }

    Heap merge(Heap that) {
        return new Heap(this.instances.merge(that.instances), this.arrays.merge(that.arrays));
    }

    Heap gc(Set<Address> roots) {
        Set<Address> reachable = reachable(roots);
        Heap heap = new Heap();
        for (Map.Entry<AddressField, ImmutableList<MyValue>> addressFieldValues : instances.entrySet()) {
            AddressField addressField = addressFieldValues.getKey();
            if (reachable.contains(addressField.address)) {
                heap.instances.putS(addressField.address, addressField.field, addressFieldValues.getValue());
            }
        }
        for (Map.Entry<Address, ImmutableList<MyValue>> addressValues : arrays.entrySet()) {
            Address address = addressValues.getKey();
            if (reachable.contains(address)) {
                heap.arrays.putS(address, addressValues.getValue());
            }
        }
        return heap;
    }

    Heap localize(Set<Address> roots, List<AddressField> addressFields, List<Address> addresses) {
        Set<Address> reachable = reachable(roots);
        Heap heap = new Heap();
        for (Map.Entry<AddressField, ImmutableList<MyValue>> addressFieldValues : instances.entrySet()) {
            AddressField addressField = addressFieldValues.getKey();
            if (reachable.contains(addressField.address) && (addressFields.contains(addressField) || addressField.field.equals(ObjectUtil.v().taint))) {
                heap.instances.putS(addressField.address, addressField.field, addressFieldValues.getValue());
            }
        }
        for (Map.Entry<Address, ImmutableList<MyValue>> addressValues : arrays.entrySet()) {
            Address address = addressValues.getKey();
            if (reachable.contains(address) && addresses.contains(address)) {
                heap.arrays.putS(address, addressValues.getValue());
            }
        }
        return heap;
    }

    boolean isEmpty() {
        return instances.isEmpty() && arrays.isEmpty();
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (!(that instanceof Heap)) {
            return false;
        }
        Heap heap = (Heap)that;

        return this.instances.equals(heap.instances) && this.arrays.equals(heap.arrays);
    }

    @Override
    public String toString() {
        return "(" + instances + ", " + arrays + ")";
    }

    private Set<Address> reachable(Set<Address> roots) {
        Set<Address> addressesReachable = new HashSet<Address>(roots);
        Set<Address> addressesToVisit = new HashSet<Address>(roots);
        Set<Address> addressesVisited = new HashSet<Address>();
        DefaultHashMap<Address, List<Address>> reachableDirectly = new DefaultHashMap<Address, List<Address>>(Collections.<Address>emptyList());
        for (Map.Entry<AddressField, ImmutableList<MyValue>> addressFieldValues : instances.entrySet()) {
            AddressField addressField = addressFieldValues.getKey();
            ArrayList<Address> addresses = new ArrayList<Address>(reachableDirectly.get(addressField.address));
            for (MyValue value : addressFieldValues.getValue()) {
                if (value instanceof Address) {
                    addresses.add((Address)value);
                }
            }
            if (!addresses.isEmpty()) {
                reachableDirectly.put(addressField.address, addresses);
            }
        }
        while (!addressesToVisit.isEmpty()) {
            Set<Address> newAddressesToVisit = new HashSet<Address>();
            for (Address address : addressesToVisit) {
                addressesVisited.add(address);
                List<Address> addresses = reachableDirectly.get(address);
                addressesReachable.addAll(addresses);
                for (Address addr : addresses) {
                    if (!addressesVisited.contains(addr)) {
                        newAddressesToVisit.add(addr);
                    }
                }
                for (MyValue value : arrays.get(address)) {
                    if (value instanceof Address) {
                        Address addr = (Address)value;
                        addressesReachable.add(addr);
                        if (!addressesVisited.contains(addr)) {
                            newAddressesToVisit.add(addr);
                        }
                    }
                }
            }
            addressesToVisit = newAddressesToVisit;
        }
        return addressesReachable;
    }

    public Heap minus(Heap that) {
        return new Heap(this.instances.minus(that.instances), this.arrays.minus(that.arrays));
    }
}

class Instances {
    private final DefaultHashMap<AddressField, ImmutableList<MyValue>> addressFieldToValues;

    Instances() {
        addressFieldToValues = new DefaultHashMap<AddressField, ImmutableList<MyValue>>(ImmutableList.<MyValue>of());
    }

    Instances(Instances that) {
        this.addressFieldToValues = new DefaultHashMap<AddressField, ImmutableList<MyValue>>(that.addressFieldToValues);
    }

    Instances merge(Instances that) {
        Instances instances = new Instances();
        for (Map.Entry<AddressField, ImmutableList<MyValue>> addressFieldValues1 : this.addressFieldToValues.entrySet()) {
            AddressField addressField = addressFieldValues1.getKey();
            ImmutableList<MyValue> values1 = addressFieldValues1.getValue();
            if (that.addressFieldToValues.containsKey(addressField)) {
                Set<MyValue> values = new HashSet<MyValue>(values1);
                values.addAll(that.addressFieldToValues.get(addressField));
                instances.addressFieldToValues.put(addressField, ImmutableList.copyOf(values));
            } else {
                instances.addressFieldToValues.put(addressField, values1);
            }
        }
        for (Map.Entry<AddressField, ImmutableList<MyValue>> addressFieldValues2 : that.addressFieldToValues.entrySet()) {
            AddressField addressField = addressFieldValues2.getKey();
            if (!this.addressFieldToValues.containsKey(addressField)) {
                instances.addressFieldToValues.put(addressField, addressFieldValues2.getValue());
            }
        }
        return instances;
    }

    // weakly update
    ImmutableList<MyValue> putW(Address address, SootField field, Set<MyValue> values) {
        return putW(address, field, ImmutableList.copyOf(values));
    }

    ImmutableList<MyValue> putW(Address address, SootField field, List<MyValue> values) {
        AddressField addressField = AddressField.v(address, field);
        ImmutableList<MyValue> valuesOld = addressFieldToValues.get(addressField);
        if (values != null && !values.isEmpty()) {
            Set<MyValue> valuesNew = new HashSet<MyValue>(valuesOld);
            valuesNew.addAll(values);
            return addressFieldToValues.put(addressField, ImmutableList.copyOf(valuesNew));
        } else {
            return valuesOld;
        }
    }

    // strongly update
    ImmutableList<MyValue> putS(AddressField addressField, ImmutableList<MyValue> values) {
        if (values != null && !values.isEmpty()) {
            return addressFieldToValues.put(addressField, values);
        } else {
            return addressFieldToValues.remove(addressField);
        }
    }

    // strongly update
    ImmutableList<MyValue> putS(Address address, SootField field, ImmutableList<MyValue> values) {
        AddressField addressField = AddressField.v(address, field);
        if (values != null && !values.isEmpty()) {
            return addressFieldToValues.put(addressField, values);
        } else {
            return addressFieldToValues.remove(addressField);
        }
    }

    ImmutableList<MyValue> get(AddressField addressField) {
        return addressFieldToValues.get(addressField);
    }

    ImmutableList<MyValue> get(Address address, SootField field) {
        return addressFieldToValues.get(AddressField.v(address, field));
    }

    Set<Map.Entry<AddressField, ImmutableList<MyValue>>> entrySet() {
        return addressFieldToValues.entrySet();
    }

    boolean isEmpty() {
        return addressFieldToValues.isEmpty();
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (!(that instanceof Instances)) {
            return false;
        }
        Instances instances = (Instances)that;

        if (this.addressFieldToValues.size() != instances.addressFieldToValues.size()) {
            return false;
        }
        try {
            for (Map.Entry<AddressField, ImmutableList<MyValue>> addressFieldValues : this.addressFieldToValues.entrySet()) {
                AddressField addressField = addressFieldValues.getKey();
                ImmutableList<MyValue> values = addressFieldValues.getValue();
                if (!(ImmutableSet.copyOf(values).equals(ImmutableSet.copyOf(instances.addressFieldToValues.get(addressField))))) {
                    return false;
                }
            }
        } catch (ClassCastException unused) {
            return false;
        } catch (NullPointerException unused) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return new TreeMap<AddressField, ImmutableList<MyValue>>(addressFieldToValues).toString();
    }

    public Instances minus(Instances that) {
        Instances instances = new Instances();
        for (Map.Entry<AddressField, ImmutableList<MyValue>> addressFieldValues : this.addressFieldToValues.entrySet()) {
            AddressField addressField = addressFieldValues.getKey();
            Set<MyValue> values = new HashSet<MyValue>(addressFieldValues.getValue());
            values.removeAll(that.get(addressField));
            instances.putS(addressField, ImmutableList.<MyValue>copyOf(values));
        }
        return instances;
    }
}

class AddressField implements Comparable<AddressField> {
    private static final HashMap<ImmutablePair<Address, SootField>, AddressField> cache = new HashMap<ImmutablePair<Address, SootField>, AddressField>();

    Address address;
    SootField field;

    private AddressField(Address address, SootField field) {
        assert address != null && field != null;
        this.address = address;
        this.field = field;
    }

    static AddressField v(Address address, SootField field) {
        ImmutablePair<Address, SootField> key = ImmutablePair.of(address, field);
        AddressField addressField = cache.get(key);
        if (addressField == null) {
            addressField = new AddressField(address, field);
            cache.put(key, addressField);
        }
        return addressField;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (!(that instanceof AddressField)) {
            return false;
        }
        AddressField addressField = (AddressField)that;

        return this.address.equals(addressField.address) && this.field.equals(addressField.field);
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }

    @Override
    public String toString() {
        return "(" + address + ", " + field.getSignature() + ")";
    }

    @Override
    public int compareTo(AddressField that) {
        int address = this.address.compareTo(that.address);
        if (address != 0) {
            return address;
        } else {
            return this.field.getSignature().compareTo(that.field.getSignature());
        }
    }
}

class Address implements MyValue, Comparable<Address> {
    private static Map<AllocNode, Address> allocNodeToAddress = new HashMap<AllocNode, Address>();

    private final AllocNode allocNode;

    private Address(AllocNode allocNode) {
        assert allocNode != null;
        this.allocNode = allocNode;
    }

    public static Address v(AllocNode allocNode) {
        assert allocNode != null;
        Address address = allocNodeToAddress.get(allocNode);
        if (address == null) {
            address = new Address(allocNode);
            allocNodeToAddress.put(allocNode, address);
        }
        return address;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (!(that instanceof Address)) {
            return false;
        }
        Address address = (Address)that;

        return this.allocNode.equals(address.allocNode);
    }

    @Override
    public int hashCode() {
        return allocNode.hashCode();
    }

    @Override
    public String toString() {
        return allocNode.toString();
    }

    @Override
    public int compareTo(Address that) {
        return allocNode.toString().compareTo(that.allocNode.toString());
    }
}

class Arrays {
    private final DefaultHashMap<Address, ImmutableList<MyValue>> addressToValues;

    Arrays() {
        addressToValues = new DefaultHashMap<Address, ImmutableList<MyValue>>(ImmutableList.<MyValue>of());
    }

    Arrays(Arrays that) {
        this.addressToValues = new DefaultHashMap<Address, ImmutableList<MyValue>>(that.addressToValues);
    }

    Arrays merge(Arrays that) {
        Arrays arrays = new Arrays();
        for (Map.Entry<Address, ImmutableList<MyValue>> addressValues1 : this.addressToValues.entrySet()) {
            Address address = addressValues1.getKey();
            ImmutableList<MyValue> values1 = addressValues1.getValue();
            if (that.addressToValues.containsKey(address)) {
                Set<MyValue> values = new HashSet<MyValue>(values1);
                values.addAll(that.addressToValues.get(address));
                arrays.addressToValues.put(address, ImmutableList.copyOf(values));
            } else {
                arrays.addressToValues.put(address, values1);
            }
        }
        for (Map.Entry<Address, ImmutableList<MyValue>> addressValues2 : that.addressToValues.entrySet()) {
            Address address = addressValues2.getKey();
            if (!this.addressToValues.containsKey(address)) {
                arrays.addressToValues.put(address, addressValues2.getValue());
            }
        }
        return arrays;
    }

    ImmutableList<MyValue> putS(Address address, ImmutableList<MyValue> values) {
        if (values != null && !values.isEmpty()) {
            return addressToValues.put(address, values);
        } else {
            return addressToValues.remove(address);
        }
    }

    ImmutableList<MyValue> putW(Address address, List<MyValue> values) {
        ImmutableList<MyValue> valuesOld = addressToValues.get(address);
        if (values != null && !values.isEmpty()) {
            Set<MyValue> valuesNew = new HashSet<MyValue>(valuesOld);
            valuesNew.addAll(values);
            return addressToValues.put(address, ImmutableList.copyOf(valuesNew));
        } else {
            return valuesOld;
        }
    }

    ImmutableList<MyValue> get(Address address) {
        return addressToValues.get(address);
    }

    Set<Map.Entry<Address, ImmutableList<MyValue>>> entrySet() {
        return addressToValues.entrySet();
    }

    boolean isEmpty() {
        return addressToValues.isEmpty();
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (!(that instanceof Arrays)) {
            return false;
        }
        Arrays arrays = (Arrays)that;

        if (this.addressToValues.size() != arrays.addressToValues.size()) {
            return false;
        }
        try {
            for (Map.Entry<Address, ImmutableList<MyValue>> addressValues : this.addressToValues.entrySet()) {
                Address address = addressValues.getKey();
                ImmutableList<MyValue> values = addressValues.getValue();
                if (!(ImmutableSet.copyOf(values).equals(ImmutableSet.copyOf(arrays.addressToValues.get(address))))) {
                    return false;
                }
            }
        } catch (ClassCastException unused) {
            return false;
        } catch (NullPointerException unused) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return new TreeMap<Address, ImmutableList<MyValue>>(addressToValues).toString();
    }

    public Arrays minus(Arrays that) {
        Arrays arrays = new Arrays();
        for (Map.Entry<Address, ImmutableList<MyValue>> addressValues : this.addressToValues.entrySet()) {
            Address address = addressValues.getKey();
            Set<MyValue> values = new HashSet<MyValue>(addressValues.getValue());
            values.removeAll(that.get(address));
            arrays.putS(address, ImmutableList.<MyValue>copyOf(values));
        }
        return arrays;
    }
}

class Statics {
    private final DefaultHashMap<SootField, ImmutableList<MyValue>> fieldToValues;

    Statics() {
        fieldToValues = new DefaultHashMap<SootField, ImmutableList<MyValue>>(ImmutableList.<MyValue>of());
    }

    Statics(Statics that) {
        fieldToValues = new DefaultHashMap<SootField, ImmutableList<MyValue>>(that.fieldToValues);
    }

    Statics merge(Statics that) {
        Statics statics = new Statics();
        for (Map.Entry<SootField, ImmutableList<MyValue>> fieldValues1 : this.fieldToValues.entrySet()) {
            SootField field = fieldValues1.getKey();
            ImmutableList<MyValue> values1 = fieldValues1.getValue();
            if (that.fieldToValues.containsKey(field)) {
                Set<MyValue> values = new HashSet<MyValue>(values1);
                values.addAll(that.fieldToValues.get(field));
                statics.fieldToValues.put(field, ImmutableList.copyOf(values));
            } else {
                statics.fieldToValues.put(field, values1);
            }
        }
        for (Map.Entry<SootField, ImmutableList<MyValue>> fieldValues2 : that.fieldToValues.entrySet()) {
            SootField field = fieldValues2.getKey();
            if (!this.fieldToValues.containsKey(field)) {
                statics.fieldToValues.put(field, fieldValues2.getValue());
            }
        }
        return statics;
    }

    // weakly update
    ImmutableList<MyValue> putW(SootField field, List<MyValue> values) {
        ImmutableList<MyValue> valuesOld = fieldToValues.get(field);
        if (values != null && !values.isEmpty()) {
            Set<MyValue> valuesNew = new HashSet<MyValue>(valuesOld);
            valuesNew.addAll(values);
            return fieldToValues.put(field, ImmutableList.copyOf(valuesNew));
        } else {
            return valuesOld;
        }
    }

    // strongly update
    ImmutableList<MyValue> putS(SootField field, ImmutableList<MyValue> values) {
        if (values != null && !values.isEmpty()) {
            return fieldToValues.put(field, values);
        } else {
            return fieldToValues.remove(field);
        }
    }

    ImmutableList<MyValue> remove(SootField field) {
        return fieldToValues.remove(field);
    }

    ImmutableList<MyValue> get(SootField field) {
        return fieldToValues.get(field);
    }

    Set<Address> roots() {
        Set<Address> roots = new HashSet<Address>();
        for (ImmutableList<MyValue> values : fieldToValues.values()) {
            for (MyValue value : values) {
                if (value instanceof Address) {
                    roots.add((Address)value);
                }
            }
        }
        return roots;
    }

    Statics localize(List<SootField> fields) {
        Statics statics = new Statics();
        for (SootField field : fieldToValues.keySet()) {
            if (fields.contains(field)) {
                statics.putS(field, fieldToValues.get(field));
            }
        }
        return statics;
    }

    boolean isEmpty() {
        return fieldToValues.isEmpty();
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (!(that instanceof Statics)) {
            return false;
        }
        Statics statics = (Statics)that;

        if (this.fieldToValues.size() != statics.fieldToValues.size()) {
            return false;
        }
        try {
            for (Map.Entry<SootField, ImmutableList<MyValue>> fieldValues : this.fieldToValues.entrySet()) {
                SootField field = fieldValues.getKey();
                ImmutableList<MyValue> values = fieldValues.getValue();
                if (!(ImmutableSet.copyOf(values).equals(ImmutableSet.copyOf(statics.fieldToValues.get(field))))) {
                    return false;
                }
            }
        } catch (ClassCastException unused) {
            return false;
        } catch (NullPointerException unused) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        Comparator<SootField> fieldComparator = new Comparator<SootField>() {
            @Override
            public int compare(SootField field1, SootField field2) {
                return field1.getSignature().compareTo(field2.getSignature());
            }
        };
        TreeMap<SootField, ImmutableList<MyValue>> sortedFieldToValues = new TreeMap<SootField, ImmutableList<MyValue>>(fieldComparator);
        sortedFieldToValues.putAll(fieldToValues);
        return sortedFieldToValues.toString();
    }

    public Statics minus(Statics that) {
        Statics statics = new Statics();
        for (Map.Entry<SootField, ImmutableList<MyValue>> fieldValues : this.fieldToValues.entrySet()) {
            SootField field = fieldValues.getKey();
            Set<MyValue> values = new HashSet<MyValue>(fieldValues.getValue());
            values.removeAll(that.get(field));
            statics.putS(field, ImmutableList.<MyValue>copyOf(values));
        }
        return statics;
    }
}

interface MyValue {
    // empty
}
