package polyglot.ext.siggen.reflect;

import polyglot.types.ParsedClassType;

public class InlineSignatureClass extends SignatureClass {

	public InlineSignatureClass(SignatureClass containingClass,
			ParsedClassType type, boolean shouldParameterize) {
		super(containingClass, type, shouldParameterize);
	}
	
	@Override
	public String getShortNameForClass() {
		return "Inline" + super.getShortNameForClass();
	}
	
	@Override
	protected String getSignatureIndicatorString() {
		return "";
	}
}
