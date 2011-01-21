package polyglot.ext.siggen.util;

import java.util.Comparator;

import polyglot.types.ParsedClassType;

public class PolyglotNamedComparator implements Comparator<ParsedClassType> {

	public int compare(ParsedClassType ct1, ParsedClassType ct2) {
		return ct1.fullName().compareTo(ct2.fullName());
	}
}
