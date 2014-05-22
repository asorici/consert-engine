package org.aimas.ami.contextrep.core;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.aimas.ami.contextrep.exceptions.ConfigException;
import org.aimas.ami.contextrep.model.BinaryContextAssertion;
import org.aimas.ami.contextrep.model.ConstraintsWrapper;
import org.aimas.ami.contextrep.model.ContextAssertion;
import org.aimas.ami.contextrep.model.ContextAssertion.ContextAssertionType;
import org.aimas.ami.contextrep.model.ContextAssertionGraph;
import org.aimas.ami.contextrep.model.DerivedAssertionWrapper;
import org.aimas.ami.contextrep.model.impl.ContextAssertionImpl;
import org.aimas.ami.contextrep.utils.ContextAssertionFinder;
import org.aimas.ami.contextrep.utils.ContextAssertionUtil;
import org.aimas.ami.contextrep.utils.spin.ContextSPINQueryFinder;
import org.aimas.ami.contextrep.vocabulary.ConsertCore;
import org.topbraid.spin.model.Construct;
import org.topbraid.spin.model.ElementList;
import org.topbraid.spin.model.SPINFactory;
import org.topbraid.spin.model.Template;
import org.topbraid.spin.model.TemplateCall;
import org.topbraid.spin.model.TripleTemplate;
import org.topbraid.spin.util.CommandWrapper;
import org.topbraid.spin.util.SPINQueryFinder;
import org.topbraid.spin.vocabulary.SPIN;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.tdb.TDB;
import com.hp.hpl.jena.tdb.base.file.Location;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

public class Loader {
	private static Properties configProperties;
	
	public static String DEFAULT_ASSEMBLER_FILE = "etc/context-tdb-assembler.ttl";
	public static String DEFAULT_STORAGE_DIRECTORY = "store";
	public static String DEFAULT_DOC_MGR_PATH = "etc/context-ont-policy.rdf;etc/context-ont-location-mapper.ttl";
	
	public static void parseConfigProperties(String configurationFile) throws ConfigException {
		// load the properties file
		configProperties = new Properties();
		
		try {
			configProperties.load(new FileInputStream(configurationFile));
		} catch (FileNotFoundException e) {
			throw new ConfigException("configuration.properties file not found", e);
		} catch (IOException e) {
			throw new ConfigException("Error reading configuration.properties file.", e);
		}
	}
	
	/**
	 * Setup the global ontology document manager with the path parsed from the config.properties file
	 * and return the context model domain ontology base URI
	 * @return The base URI of the context model domain ontology
	 */
	public static String setupOntologyDocManager() throws ConfigException {
		String docMgrPath = configProperties.getProperty("context.model.documentmgr.path", DEFAULT_DOC_MGR_PATH);
		String contextModelURI = configProperties.getProperty("context.model.ontology.uri");
		if (contextModelURI == null) {
			throw new ConfigException("Key context.model.ontology.uri not found in configuration properties file");
		}
		
		OntDocumentManager globalDocMgr = OntDocumentManager.getInstance();
		globalDocMgr.setMetadataSearchPath(docMgrPath, true);
		
		return contextModelURI;
	}
	
	
	public static int getInsertThreadPoolSize() throws ConfigException {
		String sizeStr = configProperties.getProperty("context.impl.insertion.threadpool.size", "1");
		
		try {
			return Integer.parseInt(sizeStr);
		}
		catch(NumberFormatException e) {
			throw new ConfigException("Illegal specification for integer size of insertion thread pool", e);
		}
	}
	
	
	public static int getInferenceThreadPoolSize() throws ConfigException {
		String sizeStr = configProperties.getProperty("context.impl.inference.threadpool.size", "1");
		
		try {
			return Integer.parseInt(sizeStr);
		}
		catch(NumberFormatException e) {
			throw new ConfigException("Illegal specification for integer size of inference thread pool", e);
		}
	}
	
	
	/** Create or connect to an in-memory dataset that contains:
	 * <ul>
	 * 	<li> the general store: statement of the context model that refer
	 *   	 to context entities (rdf:type statements and EntityDescriptions) </li>
	 * 	<li> the named graphs that act as Stores for each TYPE of ContextAssertion </li>
	 * 	<li> the named graphs which act as identifiers of the ContextAssertions </li>
	 * </ul>
	 * 
	 * The default storage directory is: <i>"store"</i>
	 *  
	 * @return The {@link Location} of the in-memory TDB backed Dataset that stores the ContextEntities, ContextAssertions and their Annotations
	 * @throws ConfigException if the configuration properties are not initialized (usually because the 
	 * <i>configuration.properties</i> was not found) 
	 */
	public static Location createOrOpenTDB() throws ConfigException {
		if (configProperties != null) {
			String tdbStorageDirectory = 
				configProperties.getProperty("tdb.storage.directory", DEFAULT_STORAGE_DIRECTORY);
			
			TDB.init();
			//TDBFactory.createDataset(tdbStorageDirectory);
			
			return Location.mem(tdbStorageDirectory);
			//return new Location(tdbStorageDirectory);
		}
		
		throw new ConfigException();
	}
	
	/**
	 * Create the named graph that acts as the store for ContextEntity and EntityDescription instances
	 * @param dataset The TDB-backed dataset that holds the graphs
	 * @return The URI identifying
	 * @throws ConfigException if the configProperties could not be parsed from file or the file does not contain
	 * the settings for the context entity store are not given
	 */
	public static String createEntityStoreGraph(Dataset dataset) throws ConfigException {
		if (configProperties != null) {
			String entityStoreURI = configProperties.getProperty("context.model.ontology.entitystore.uri");
			
			if (entityStoreURI != null) {
				Model graphStore = dataset.getNamedModel(entityStoreURI);
				TDB.sync(graphStore);
				
				return entityStoreURI;
			}
			
			throw new ConfigException("Property context.model.ontology.entitystore.uri not " +
					"found in configuration file.");
		}
		
		throw new ConfigException();
	}
	
	
	/**
	 * Create the index structure that retains all types of ContextAssertion and the mapping
	 * between a ContextAssertion and the named graph URI that acts as Store for that type of ContextAssertion
	 * @param transitiveContextModel The ontology (with transitive inference enabled) that defines this context model
	 * @param dataset The TDB-backed dataset that holds the graphs
	 * @return An {@link ContextAssertionIndex} instance that holds the index structure.
	 */
	public static ContextAssertionIndex createContextAssertionIndex(OntModel transitiveContextModel) {
		
		ContextAssertionIndex assertionIndex = new ContextAssertionIndex();
		
		// search all EntityRelationAssertions
		ExtendedIterator<? extends OntProperty> relationPropIt = 
			transitiveContextModel.getOntProperty(ConsertCore.ENTITY_RELATION_ASSERTION.getURI()).listSubProperties();
		
		for (; relationPropIt.hasNext();) {
			OntProperty prop = relationPropIt.next();
			if (!SPINFactory.isAbstract(prop)) {
				ContextAssertionType assertionType = ContextAssertionUtil.getType(prop, transitiveContextModel);
				ContextAssertion assertion = ContextAssertionImpl.createBinary(assertionType, 2, prop);
				
				switch(assertionType) {
				case Static:
					assertionIndex.addStaticContextAssertion(assertion);
					break;
				case Profiled:
					assertionIndex.addProfiledContextAssertion(assertion);
					break;
				case Sensed:
					assertionIndex.addSensedContextAssertion(assertion);
					break;
				case Derived:
					assertionIndex.addDerivedContextAssertion(assertion);
					break;
				default:
					break;
				}
				
				assertionIndex.mapAssertionStorage(assertion);
			}
		}
		
		// search all EntityDataAssertions
		ExtendedIterator<? extends OntProperty> dataPropIt = 
			transitiveContextModel.getOntProperty(ConsertCore.ENTITY_DATA_ASSERTION.getURI()).listSubProperties();
		
		for (; dataPropIt.hasNext();) {
			OntProperty prop = dataPropIt.next();
			if (!SPINFactory.isAbstract(prop)) {
				ContextAssertionType assertionType = ContextAssertionUtil.getType(prop, transitiveContextModel);
				ContextAssertion assertion = ContextAssertionImpl.createBinary(assertionType, 2, prop);
				
				switch(assertionType) {
				case Static:
					assertionIndex.addStaticContextAssertion(assertion);
					break;
				case Profiled:
					assertionIndex.addProfiledContextAssertion(assertion);
					break;
				case Sensed:
					assertionIndex.addSensedContextAssertion(assertion);
					break;
				case Derived:
					assertionIndex.addDerivedContextAssertion(assertion);
					break;
				default:
					break;
				}
				
				assertionIndex.mapAssertionStorage(assertion);
			}
		}

		// create stores for subclasses of UnaryContextAssertion 
		ExtendedIterator<OntClass> unaryClassIt = transitiveContextModel
			.getOntClass(ConsertCore.UNARY_CONTEXT_ASSERTION.getURI()).listSubClasses();
		for (; unaryClassIt.hasNext();) {
			OntClass cls = unaryClassIt.next();
			if (!SPINFactory.isAbstract(cls)) {
				ContextAssertionType assertionType = ContextAssertionUtil.getType(cls, transitiveContextModel);
				ContextAssertion assertion = ContextAssertionImpl.createUnary(assertionType, 1, cls, transitiveContextModel);
				
				switch(assertionType) {
				case Static:
					assertionIndex.addStaticContextAssertion(assertion);
					break;
				case Profiled:
					assertionIndex.addProfiledContextAssertion(assertion);
					break;
				case Sensed:
					assertionIndex.addSensedContextAssertion(assertion);
					break;
				case Derived:
					assertionIndex.addDerivedContextAssertion(assertion);
					break;
				default:
					break;
				}
				
				assertionIndex.mapAssertionStorage(assertion);
			}
		}
		
		// and subclasses of NaryContextAssertion
		ExtendedIterator<OntClass> naryClassIt = transitiveContextModel
			.getOntClass(ConsertCore.NARY_CONTEXT_ASSERTION.getURI()).listSubClasses();
		for (; naryClassIt.hasNext();) {
			OntClass cls = naryClassIt.next();
			if (!SPINFactory.isAbstract(cls)) {
				ContextAssertionType assertionType = ContextAssertionUtil.getType(cls, transitiveContextModel);
				ContextAssertion assertion = ContextAssertionImpl.createNary(assertionType, 3, cls, transitiveContextModel);
				
				switch(assertionType) {
				case Static:
					assertionIndex.addStaticContextAssertion(assertion);
					break;
				case Profiled:
					assertionIndex.addProfiledContextAssertion(assertion);
					break;
				case Sensed:
					assertionIndex.addSensedContextAssertion(assertion);
					break;
				case Derived:
					assertionIndex.addDerivedContextAssertion(assertion);
					break;
				default:
					break;
				}
				
				assertionIndex.mapAssertionStorage(assertion);
			}
		}
		
		return assertionIndex;
	}
	
	/**
	 * Create the basic (no inference added) {@link OntModel} that holds the domain context model.
	 * The default document manager specification search path is set to: 
	 * <i>"etc/context-ont-policy.rdf;etc/context-ont-location-mapper.ttl"</i> 
	 * 
	 * @return The OntModel that holds the domain context model. No inference is attached to this Ontology Model.
	 *  
	 * @throws if the configuration properties are not initialized (usually because the 
	 * <i>configuration.properties</i> was not found)
	 */
	public static OntModel getBasicContextModel() throws ConfigException {
		if (configProperties != null) {
			// String docMgrPath = configProperties.getProperty("context.model.documentmgr.path", DEFAULT_DOC_MGR_PATH);
			String contextModelURI = configProperties.getProperty("context.model.ontology.uri");
			if (contextModelURI == null) {
				throw new ConfigException("Key context.model.ontology.uri not found in configuration properties file");
			}
			
			OntModelSpec contextModelSpec = new OntModelSpec(OntModelSpec.OWL_MEM);
			OntModel contextModel = ModelFactory.createOntologyModel(contextModelSpec);
			
			contextModel.read(contextModelURI);
			
			return contextModel;
		}
		
		throw new ConfigException();
	}
	
	
	public static OntModel getTransitiveInferenceModel(OntModel basicContextModel) {
		return ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_TRANS_INF, basicContextModel);
	}
	
	public static OntModel getRDFSInferenceModel(OntModel basicContextModel) {
		return ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_RDFS_INF, basicContextModel);
	}
	
	public static OntModel getOWLInferenceModel(OntModel basicContextModel) {
		return ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_MINI_RULE_INF, basicContextModel);
	}
	
	/**
	 * Create a DerivationRule dictionary which maps each ContextAssertion to the SPIN rules 
	 * in which it plays a role. The SPIN rules are selected from those attached
	 * by a <code>spin:deriveassertion</code> property to a ContextEntity of the context model given
	 * by <code>basicContextModel</code>.
	 * @param transitiveContextModel The ontology defining the context model, with no attached inference rules
	 * @return A {@link DerivationRuleDictionary} instance which contains maps of ContextEntity to 
	 * list of SPIN Rules and ContextAssertion to list of SPIN Rules.
	 */
	public static DerivationRuleDictionary buildDerivationRuleDictionary(OntModel transitiveContextModel) {
		DerivationRuleDictionary dict = new DerivationRuleDictionary();
		ContextAssertionIndex assertionIndex = Config.getContextAssertionIndex();
		
		/*
		 * Collect spin:deriveassertion rules in basicContextModel
		 */
		Property deriveAssertionProp = ResourceFactory.createProperty(SPIN.NS + "deriveassertion");
		
		//Map<CommandWrapper, Map<String,RDFNode>> initialTemplateBindings = new HashMap<CommandWrapper, Map<String,RDFNode>>();
		//Map<Resource,List<CommandWrapper>> cls2Query = SPINQueryFinder.getClass2QueryMap(
		//		transitiveContextModel, transitiveContextModel, deriveAssertionProp, false, initialTemplateBindings, false);
		
		Map<Resource,List<CommandWrapper>> cls2Query = SPINQueryFinder.getClass2QueryMap(
				transitiveContextModel, transitiveContextModel, deriveAssertionProp, false, false);
		
		
		// build map for ContextAssertion to SPIN:Rule list
		for (Resource res : cls2Query.keySet()) {
			//System.out.println(res.getURI() + ": ");
			//System.out.println(res.getClass().getName() + ": ");
			List<CommandWrapper> cmds = cls2Query.get(res);
			
			for (CommandWrapper cmd : cmds) {
				org.topbraid.spin.model.Command spinCommand = null;
				Template template = null;
				
				TemplateCall templateCall = SPINFactory.asTemplateCall(cmd.getSource());
				if (templateCall != null) {
					template = templateCall.getTemplate();
					if(template != null) {
						spinCommand = template.getBody();
					}
				}
				else {
					spinCommand = SPINFactory.asCommand(cmd.getSource());
				}
				
				
				/*
				 * The derivation rules are constructed as sp:Construct statement with a SP.templates predicate
				 * defining the TemplateTriples in the head of the CONSTRUCT statement
				 */
				Construct constructCommand =  spinCommand.as(Construct.class);
				ElementList whereElements = constructCommand.getWhere();
				List<TripleTemplate> constructedTriples = constructCommand.getTemplates();
				
				//Map<String, RDFNode> templateBindings = initialTemplateBindings.get(cmd);
				Map<String, RDFNode> templateBindings = cmd.getTemplateBinding();
				ContextAssertion derivedAssertion = null; 
				
				ContextAssertionFinder ruleBodyFinder = 
						new ContextAssertionFinder(whereElements, transitiveContextModel, templateBindings);
				
				// run context assertion rule body finder and collect results
				ruleBodyFinder.run();
				List<ContextAssertionGraph> bodyContextAssertions = ruleBodyFinder.getResult();
				
				// look through asserted triples as part of the CONSTRUCT for the derived assertion and retrieve
				// its resource type
				for (TripleTemplate tpl : constructedTriples) {
					if (tpl.getPredicate().equals(
						transitiveContextModel.getProperty(ConsertCore.CONTEXT_ASSERTION_RESOURCE.getURI()))) {
						
						RDFNode assertionRes = tpl.getObjectResource();
						if (SPINFactory.isVariable(assertionRes)) {
							String varName = SPINFactory.asVariable(assertionRes).getName();
							if (templateBindings != null && templateBindings.get(varName) != null) { 
								assertionRes = templateBindings.get(varName);
							}
						}
						
						OntResource derivedAssertionResource = transitiveContextModel.getOntResource(assertionRes.asResource());
						derivedAssertion = assertionIndex.getAssertionFromResource(derivedAssertionResource);
					}
				}
				
				// there is only one head ContextAssertion - the derived one
				DerivedAssertionWrapper derivationWrapper = new DerivedAssertionWrapper(derivedAssertion, cmd, templateBindings);
				
				for (ContextAssertionGraph assertionGraph : bodyContextAssertions) {
					// System.out.println(assertion.getAssertionResource().getURI() + ": " + assertion.getAssertionType());
					dict.addDerivationForAssertion(
						assertionIndex.getAssertionFromResource(assertionGraph.getAssertionResource()), 
						derivationWrapper);
				}
				
				// add all ContextEntity to SPIN:Rule list mappings
				dict.addCommandForEntity(res, derivationWrapper);
				
				// add reverse map from SPIN:Rule to ContextEntity
				dict.setEntityForDerivation(derivationWrapper, res);
			}
		}
		
		return dict;
	}
	
	/**
	 * Create an index of uniqueness or value constraints attached to a ContextAssertion. It provides a mapping
	 * between a ContextAssertion and the list of constraints attached to it.
	 * @param rdfsContextModel The ontology (with transitive inference enabled) that defines this context model
	 * @return An {@link ContextAssertionIndex} instance that holds the index structure.
	 */
	public static ConstraintIndex buildConstraintIndex(ContextAssertionIndex contextAssertionIndex, OntModel rdfsContextModel) {
	    // create the ConstraintIndex instance
		ConstraintIndex constraintIndex = new ConstraintIndex();
		
		List<ContextAssertion> assertionList = contextAssertionIndex.getContextAssertions();
	    for (ContextAssertion assertion : assertionList) {
	    	if (assertion.isBinary()) {
	    		BinaryContextAssertion binaryAssertion = (BinaryContextAssertion) assertion;
	    		
	    		// for a binary assertion get the domain and range and see if they have any assigned constraints
	    		// we do this because for binary assertions (i.e. OWL object- or datatype properties) we
	    		// attach the constraints to one of the role playing Entities
	    		Resource domain = binaryAssertion.getDomainEntityResource();
	    		Resource range = binaryAssertion.getRangeEntityResource();
	    		
	    		//Map<CommandWrapper, Map<String,RDFNode>> initialTemplateBindings = new HashMap<CommandWrapper, Map<String,RDFNode>>();
	    		
	    		Map<Resource, List<CommandWrapper>> constraintsMap = new HashMap<>();
	    		if (domain != null && domain.isURIResource()) {
	    			Resource anchorResource = domain;
	    			//constraintsMap.putAll(ContextSPINQueryFinder.getClass2QueryMap(rdfsContextModel, rdfsContextModel, 
	    			//		anchorResource, SPIN.constraint, true, initialTemplateBindings, true));
	    			constraintsMap.putAll(ContextSPINQueryFinder.getClass2QueryMap(rdfsContextModel, rdfsContextModel, 
	    					anchorResource, SPIN.constraint, true, true));
	    		}
	    		
	    		if (range != null && range.isURIResource() && (constraintsMap == null || constraintsMap.isEmpty())) {
	    			Resource anchorResource = range;
	    			//constraintsMap.putAll(ContextSPINQueryFinder.getClass2QueryMap(rdfsContextModel, rdfsContextModel, 
	    			//		anchorResource, SPIN.constraint, true, initialTemplateBindings, true));
	    			constraintsMap.putAll(ContextSPINQueryFinder.getClass2QueryMap(rdfsContextModel, rdfsContextModel, 
	    					anchorResource, SPIN.constraint, true, true));
	    		}
	    		
	    		if (constraintsMap != null && !constraintsMap.isEmpty()) {
	    			for (Resource anchorResource : constraintsMap.keySet()) {
		    			List<CommandWrapper> constraints = constraintsMap.get(anchorResource);
		    			//List<CommandWrapper> filteredConstraints = filterBinaryConstraintsFor(constraints, binaryAssertion, rdfsContextModel, initialTemplateBindings);
		    			List<CommandWrapper> filteredConstraints = filterBinaryConstraintsFor(constraints, binaryAssertion, rdfsContextModel);
		    			
		    			//ConstraintsWrapper constraintsWrapper = new ConstraintsWrapper(filteredConstraints, anchorResource, initialTemplateBindings);
		    			ConstraintsWrapper constraintsWrapper = new ConstraintsWrapper(filteredConstraints, anchorResource);
	    				constraintIndex.addAssertionConstraint(binaryAssertion, constraintsWrapper);
	    			}
	    		}
	    	}
	    	else {
	    		// we have a Unary or Nary assertion. Both are ontology classes, so the constraint can be directly
	    		// attached to them
	    		Resource anchorResource = assertion.getOntologyResource();
	    		//Map<CommandWrapper, Map<String,RDFNode>> initialTemplateBindings = new HashMap<CommandWrapper, Map<String,RDFNode>>();
	    		
	    		//Map<Resource, List<CommandWrapper>> constraintsMap = 
	    		//	ContextSPINQueryFinder.getClass2QueryMap(rdfsContextModel, rdfsContextModel, anchorResource, SPIN.constraint, true, initialTemplateBindings, true);
	    		Map<Resource, List<CommandWrapper>> constraintsMap = 
	    			ContextSPINQueryFinder.getClass2QueryMap(rdfsContextModel, rdfsContextModel, anchorResource, SPIN.constraint, true, true);
	    		
	    		
	    		if (constraintsMap != null && !constraintsMap.isEmpty()) {
	    			List<CommandWrapper> constraints = constraintsMap.get(anchorResource);
	    			//ConstraintsWrapper constraintsWrapper = new ConstraintsWrapper(constraints, anchorResource, initialTemplateBindings);
	    			ConstraintsWrapper constraintsWrapper = new ConstraintsWrapper(constraints, anchorResource);
    				constraintIndex.addAssertionConstraint(assertion, constraintsWrapper);
	    		}
	    	}
	    }
	    
		return constraintIndex;
    }
	
	
	/**
	 * For a binary assertion the constraints are added to the subject of the Property defining the assertion.
	 * Since the collection process gathers all constraints associated to the subject Resource we must filter out
	 * the ones who do not relate to <code>binaryAssertion</code>.
	 */
	private static List<CommandWrapper> filterBinaryConstraintsFor(List<CommandWrapper> constraints,
            BinaryContextAssertion binaryAssertion, OntModel rdfsContextModel) {
		List<CommandWrapper> filteredConstraints = new ArrayList<>();
		
		for (CommandWrapper cmd : constraints) {
			org.topbraid.spin.model.Command spinCommand = null;
			Template template = null;
			
			TemplateCall templateCall = SPINFactory.asTemplateCall(cmd.getSource());
			if (templateCall != null) {
				template = templateCall.getTemplate();
				if(template != null) {
					spinCommand = template.getBody();
				}
			}
			else {
				spinCommand = SPINFactory.asCommand(cmd.getSource());
			}
		
			/*
			 * The constraints are constructed as CONSTRUCT statements that create an instance of spin:ConstraintViolation
			 */
			Construct constructCommand =  spinCommand.as(Construct.class);
			ElementList whereElements = constructCommand.getWhere();
			//Map<String, RDFNode> templateBindings = initialTemplateBindings.get(cmd);
			Map<String, RDFNode> templateBindings = cmd.getTemplateBinding();
			
			ContextAssertionFinder ruleBodyFinder = new ContextAssertionFinder(whereElements, rdfsContextModel, templateBindings);
			
			// run context assertion rule body finder and collect results
			ruleBodyFinder.run();
			List<ContextAssertionGraph> bodyContextAssertions = ruleBodyFinder.getResult();
			
			// see if the list of collected context assertions contains our binaryAssertion
			//initialTemplateBindings.remove(cmd);							// attempted remove of bindings for cmd
			for (ContextAssertionGraph assertionGraph : bodyContextAssertions) {
				if (assertionGraph.getAssertionResource().equals(binaryAssertion.getOntologyResource())) {
					// if the collected assertions really include our binaryAssertion
					filteredConstraints.add(cmd);							// add the command to the filtered ones
					//initialTemplateBindings.put(cmd, templateBindings);		// and add the mapping back to the template bindings
					break;
				}
			}
		}
		
		return filteredConstraints;
    }
}
