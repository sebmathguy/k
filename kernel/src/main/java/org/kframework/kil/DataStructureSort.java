// Copyright (c) 2013-2015 K Team. All Rights Reserved.
package org.kframework.kil;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.kframework.kil.loader.Context;

import java.io.Serializable;
import java.util.Map;


/**
 * Represents a sort which corresponds to some data structure. Each data
 * structure sort is hooked to one of the following builtin data structures:
 * Bag, List, Map or Set (an element of {@link DataStructureSort#TYPES}). Each
 * data structure sort must provide the following primitive operations:
 * <p>
 * (1) constructor: takes two data structures and constructs the union (bag,
 * map, set) or the concatenation (list);<br>
 * (2) element: takes one K term (bag, list, set) or two K terms (map) and
 * constructs an element (bag, list, set) or an entry (map);<br>
 * (3) unit: constructs the empty data structure.<br>
 * </p>
 * Additionally, a data structure sort may provide other hooked operations. Each
 * backend must implement these builtin data structures types.
 *
 * @author AndreiS
 */
public class DataStructureSort implements Serializable {

    public enum Label { CONSTRUCTOR, ELEMENT, UNIT }

    /** {@code Set} of builtin data structure types */
    public static final java.util.Set<Sort> TYPES = ImmutableSet.of(
            Sort.BAG,
            Sort.LIST,
            Sort.MAP,
            Sort.SET);

    /**
     * {@code Map} of builtin data structure types (Bag, List, Set, Map) to fundamental hooks
     * (builtin data structure constructor, element constructor, empty data structure constructor).
     * The full name of a hook is obtained by using the builtin module name as a prefix (e.g.
     * Map:__).
     */
    public static final Map<Sort, ImmutableMap<Label, String>> LABELS = ImmutableMap.of(
            Sort.BAG, ImmutableMap.of(
                    Label.CONSTRUCTOR, "concat",
                    Label.ELEMENT, "element",
                    Label.UNIT, "unit"),
            Sort.LIST, ImmutableMap.of(
                    Label.CONSTRUCTOR, "concat",
                    Label.ELEMENT, "element",
                    Label.UNIT, "unit"),
            Sort.MAP, ImmutableMap.of(
                    Label.CONSTRUCTOR, "concat",
                    Label.ELEMENT, "element",
                    Label.UNIT, "unit"),
            Sort.SET, ImmutableMap.of(
                    Label.CONSTRUCTOR, "concat",
                    Label.ELEMENT, "element",
                    Label.UNIT, "unit"));

    public static final Sort DEFAULT_LIST_SORT = Sort.LIST;
    public static final Sort DEFAULT_MAP_SORT = Sort.MAP;
    public static final Sort DEFAULT_SET_SORT = Sort.SET;
    public static final String DEFAULT_LIST_LABEL = "'_List_";
    public static final String DEFAULT_LIST_ITEM_LABEL = "'ListItem";
    public static final String DEFAULT_LIST_UNIT_LABEL = "'.List";
    public static final String DEFAULT_MAP_LABEL = "'_Map_";
    public static final String DEFAULT_MAP_ITEM_LABEL = "'_|->_";
    public static final String DEFAULT_MAP_UNIT_LABEL = "'.Map";
    public static final String DEFAULT_SET_LABEL = "'_Set_";
    public static final String DEFAULT_SET_ITEM_LABEL = "'SetItem";
    public static final String DEFAULT_SET_UNIT_LABEL = "'.Set";

    /** Name of this data structure sort. */
    private final String name;
    /** Type of the builtin data structure this sort is hooked to (an element of {@code TYPES}). */
    private final Sort type;
    /** {@code String} representation of the data structure constructor KLabel. */
    private final String constructorLabel;
    /** {@code String} representation of the data structure element KLabel*/
    private final String elementLabel;
    /** {@code String} representation of the empty data structure KLabel. */
    private final String unitLabel;
    /** {@code Map} of the remaining KLabels hooked to to builtin operations */
    private final ImmutableMap<String, String> operatorLabels;

    public DataStructureSort(
            String name,
            Sort type,
            String constructorLabel,
            String elementLabel,
            String unitLabel,
            Map<String, String> operatorLabels) {
        assert TYPES.contains(type): "unknown builtin collection type " + type;

        this.name = name;
        this.type = type;
        this.constructorLabel = constructorLabel;
        this.elementLabel = elementLabel;
        this.unitLabel = unitLabel;
        this.operatorLabels = ImmutableMap.copyOf(operatorLabels);
    }

    public String constructorLabel() {
        return constructorLabel;
    }

    public String elementLabel() {
        return elementLabel;
    }

    public String name() {
        return name;
    }

    public Map<String, String> operatorLabels() {
        return operatorLabels;
    }

    public Sort type() {
        return type;
    }

    @Deprecated
    public Sort sort() {
        return type();
    }

    public String unitLabel() {
        return unitLabel;
    }

    /**
     * Returns a term of sort List containing one ListItem element per argument.
     */
    public static Term listOf(Context context, Term... listItems) {
        DataStructureSort myList = context.getDefaultListDataStructureSort();
        if (listItems.length == 0) {
            return KApp.of(KLabelConstant.of(myList.unitLabel()));
        }
        Term result = listItems[0];
        for (int i = 1; i < listItems.length; i++) {
            result = KApp.of(KLabelConstant.of(myList.constructorLabel()), result, listItems[i]);
        }
        return result;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DataStructureSort)) return false;
        DataStructureSort ds = (DataStructureSort)o;
        return ds.name.equals(name) && ds.type.equals(type) && ds.unitLabel.equals(unitLabel) && ds.constructorLabel.equals(constructorLabel) && ds.elementLabel.equals(elementLabel) && ds.operatorLabels.equals(operatorLabels);
    }

    @Override
    public int hashCode() {
        return (name + type + unitLabel + elementLabel + constructorLabel).hashCode() * Context.HASH_PRIME + operatorLabels.hashCode();
    }
}
