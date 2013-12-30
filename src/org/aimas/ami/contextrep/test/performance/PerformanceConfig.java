package org.aimas.ami.contextrep.test.performance;

public class PerformanceConfig {
	public ContextEntityConfig contextEntities;
	public ContextAssertionConfig contextAssertions;
	public DerivationRuleConfig derivationRules;
	
	public static class ContextEntityConfig {
		public int nrTypes;
		public int nrInstances;
	}
	
	public static class ContextAssertionConfig {
		public AssertionArityConfig unary;
		public AssertionArityConfig binary;
		public AssertionArityConfig nary;
	}
	
	
	public static class AssertionArityConfig {
		public int nrTypes;
		public int nrDerived;
		public int nrInstances;
		public AnnotationConfig annotation;
	}
	
	public static class AnnotationConfig {
		public String accuracy;
		public int duration;
		public String assertedBy;
	}
	
	public static class DerivationRuleConfig {
		public DerivationScheme schemeUnary;
		public DerivationScheme schemeBinary;
		public DerivationScheme schemeNary;
	}
	
	public static class DerivationScheme {
		public DerivationBodyConfig unary;
		public DerivationBodyConfig binary;
		public DerivationBodyConfig nary;
		public String uri;
	}
	
	public static class DerivationBodyConfig {
		public int nr;
		public boolean derivedAllowed;
	}
}
