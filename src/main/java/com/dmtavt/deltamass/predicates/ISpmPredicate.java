package com.dmtavt.deltamass.predicates;

import com.dmtavt.deltamass.data.Spm;
import java.io.Serializable;
import java.util.function.Predicate;

@FunctionalInterface
public interface ISpmPredicate extends Predicate<Spm>, Serializable {

}
