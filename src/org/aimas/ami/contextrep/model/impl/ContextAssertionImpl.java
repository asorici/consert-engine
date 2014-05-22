package org.aimas.ami.contextrep.model.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.aimas.ami.contextrep.model.BinaryContextAssertion;
import org.aimas.ami.contextrep.model.ContextAssertion;
import org.aimas.ami.contextrep.model.exceptions.ContextAssertionContentException;
import org.aimas.ami.contextrep.model.exceptions.ContextAssertionModelException;
import org.aimas.ami.contextrep.vocabulary.ConsertCore;

import com.hp.hpl.jena.ontology.AllValuesFromRestriction;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

public abstract class ContextAssertionImpl implements ContextAssertion {
	protected ContextAssertionType assertionType;
	protected int assertionArity;
	protected OntResource assertionOntologyResource;
	protected String assertionStoreURI;
	
	public ContextAssertionImpl(ContextAssertionType assertionType, int assertionArity, OntResource assertionOntologyResource) {
		
		this.assertionType = assertionType;
		this.assertionArity = assertionArity;
		this.assertionOntologyResource = assertionOntologyResource;
		
		mapAssertionStorage();
	}

	private void mapAssertionStorage() {
		String assertionURI = assertionOntologyResource.getURI();
		assertionURI = assertionURI.replaceAll("#", "/");
		
		assertionStoreURI = assertionURI + "Store";
	}
	
	/* (non-Javadoc)
	 * @see org.aimas.ami.contextrep.model.impl.ContextAssertion#getAssertionType()
	 */
	@Override
    public ContextAssertionType getAssertionType() {
		return assertionType;
	}

	
	@Override
    public int getAssertionArity() {
		return assertionArity;
	}

	
	@Override
    public OntResource getOntologyResource() {
		return assertionOntologyResource;
	}
	
	
	@Override
    public String getAssertionStoreURI() {
		return assertionStoreURI;
	}
	
	
	@Override
    public boolean isUnary() {
		return assertionArity == UNARY;
	}

	@Override
    public boolean isBinary() {
		return assertionArity == BINARY;
	}
	
	
	@Override
    public boolean isNary() {
		return assertionArity == NARY;
	}
	
	@Override
	public int hashCode() {
		return assertionOntologyResource.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		return (obj instanceof ContextAssertionImpl) && 
			((ContextAssertion)obj).getOntologyResource().equals(assertionOntologyResource);
	}
	
	@Override
	public String toString() {
		String result = "{";
		result += "assertionType: " + assertionType + ", ";
		result += "assertionArity: " + assertionArity + ", ";
		result += "assertionOntologyResource: " + assertionOntologyResource;
		result += "}";
		
		return result;
	}

	
	public static ContextAssertion createUnary(ContextAssertionType assertionType, int assertionArity, 
			OntClass unaryAssertionClass, OntModel transitiveContextModel) {
	    
		Resource roleEntityResource = getUnaryRoleEntity(transitiveContextModel, unaryAssertionClass);
	    return new UnaryContextAssertionImpl(assertionType, assertionArity, unaryAssertionClass, roleEntityResource);
    }

	
	public static BinaryContextAssertion createBinary(ContextAssertionType assertionType, 
			int assertionArity, OntProperty assertionProp) {
		
		return new BinaryContextAssertionImpl(assertionType, assertionArity, assertionProp, 
				assertionProp.getDomain(), assertionProp.getRange());
    }

	
	public static ContextAssertion createNary(ContextAssertionType assertionType, int assertionArity, 
            OntClass naryAssertionClass, OntModel transitiveContextModel) {
		
		Map<OntProperty, Resource> assertionRoleMap = getNaryRoleMap(transitiveContextModel, naryAssertionClass);
		return new NaryContextAssertionImpl(assertionType, assertionArity, naryAssertionClass, assertionRoleMap);
    }
	
	
	private static Resource getUnaryRoleEntity(OntModel contextModel, OntClass unaryAssertion) {
		OntProperty assertionRoleProperty = contextModel.getOntProperty(ConsertCore.CONTEXT_ASSERTION_ROLE.getURI());
		
		Iterator<OntClass> supers = unaryAssertion.listSuperClasses(true);
		for(;supers.hasNext();) {
			OntClass sup = supers.next();
			if (sup.isRestriction()) {
				Restriction restriction = sup.asRestriction();
				if (restriction.isAllValuesFromRestriction()) {
					AllValuesFromRestriction avfr = restriction.asAllValuesFromRestriction();
					if (avfr.onProperty(assertionRoleProperty)) {
						return avfr.getAllValuesFrom();
					}
				}
			}
		}
		
		return null;
	}
	
	
	private static Map<OntProperty, Resource> getNaryRoleMap(OntModel contextModel, OntClass naryAssertion) {
		OntProperty assertionRoleProperty = contextModel.getOntProperty(ConsertCore.CONTEXT_ASSERTION_ROLE.getURI());
		
		ExtendedIterator<? extends OntProperty> roleProperties = assertionRoleProperty.listSubProperties(false);
		Map<OntProperty, Resource> assertionRoleMap = new HashMap<>(); 
		
		for(;roleProperties.hasNext();) {
			OntProperty prop = roleProperties.next();
			if (prop.getDomain().equals(naryAssertion)) {
				assertionRoleMap.put(prop, prop.getRange());
			}
		}
		
		return assertionRoleMap;
	}

	@Override
    public List<Statement> getAssertionContent(Resource assertionUUIDRes, Dataset contextStoreDataset) {
		// get the model store containing the ContextAssertion contents
		Model assertionContentStore = contextStoreDataset.getNamedModel(assertionUUIDRes.getURI());
		return assertionContentStore.listStatements().toList();
    } 

	@Override
    public abstract List<Statement> copyToAncestor(Resource assertionUUID, Dataset contextStoreDataset, 
    	ContextAssertion ancestorAssertion, OntModel contextModel) 
    	throws ContextAssertionContentException, ContextAssertionModelException;

}
