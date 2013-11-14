package org.aimas.ami.contextrep.model.impl;

import org.aimas.ami.contextrep.model.ContextEntity;
import org.topbraid.spin.model.Variable;

import com.hp.hpl.jena.rdf.model.RDFNode;

public class ContextEntityImpl implements ContextEntity {
	private RDFNode wrappedNode;
	
	public ContextEntityImpl(RDFNode wrappedNode) {
		this.wrappedNode = wrappedNode;
	}
	
	@Override
	public boolean isResource() {
		return wrappedNode.isResource();
	}

	@Override
	public boolean isVariable() {
		return wrappedNode.canAs(Variable.class);
	}
	
	@Override
	public boolean isLiteral() {
		return wrappedNode.isLiteral();
	}

	@Override
	public RDFNode getWrappedNode() {
		return wrappedNode;
	}

}
