package org.aimas.ami.contextrep.model;

import org.topbraid.spin.util.CommandWrapper;

import com.hp.hpl.jena.ontology.OntResource;

public class DerivedAssertionWrapper {
	
	private CommandWrapper derivationCommand;
	private OntResource derivedResource;
	
	public DerivedAssertionWrapper(OntResource derivedResource, CommandWrapper derivationCommand) {
		this.derivedResource = derivedResource;
		this.derivationCommand = derivationCommand;
    }

	public CommandWrapper getDerivationCommand() {
		return derivationCommand;
	}

	public OntResource getDerivedResource() {
		return derivedResource;
	}
	
	@Override
	public int hashCode() {
		return derivationCommand.hashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		return (other != null && other instanceof DerivedAssertionWrapper && 
			derivationCommand.equals(((DerivedAssertionWrapper)other).getDerivationCommand()));
	}
}
