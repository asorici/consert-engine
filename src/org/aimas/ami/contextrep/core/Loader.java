package org.aimas.ami.contextrep.core;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.aimas.ami.contextrep.exceptions.ConfigException;
import org.aimas.ami.contextrep.model.ContextAssertion;
import org.aimas.ami.contextrep.model.ContextAssertion.ContextAssertionType;
import org.aimas.ami.contextrep.model.ContextAssertionInfo;
import org.aimas.ami.contextrep.model.DerivedAssertionWrapper;
import org.aimas.ami.contextrep.utils.ContextAssertionFinder;
import org.aimas.ami.contextrep.utils.ContextAssertionUtil;
import org.aimas.ami.contextrep.vocabulary.ContextAssertionVocabulary;
import org.topbraid.spin.model.Construct;
import org.topbraid.spin.model.Element;
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
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.tdb.base.file.Location;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

public class Loader {
	private static Properties configProperties;
	
	private static String DEFAULT_ASSEMBLER_FILE = "etc/context-tdb-assembler.ttl";
	private static String DEFAULT_STORAGE_DIRECTORY = "store";
	private static String DEFAULT_DOC_MGR_PATH = "etc/context-ont-policy.rdf;etc/context-ont-location-mapper.ttl";
	
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
	
	/** Assemble dataset that contains:
	 * <ul>
	 * 	<li> the general store: statement of the context model that refer
	 *   	 to context entities (rdf:type statements and EntityDescriptions) </li>
	 * 	<li> the named graphs that act as Stores for each TYPE of ContextAssertion </li>
	 * 	<li> the named graphs which act as identifiers of the ContextAssertions </li>
	 * </ul>
	 * 
	 * The default assembler file location is: <i>"etc/context-tdb-assembler.ttl"</i>
	 * The default storage directory is: <i>"store"</i>
	 *  
	 * @return The {@link Location} of the TDB backed Dataset that stores the ContextEntities, ContextAssertions and their Annotations
	 * @throws ConfigException if the configuration properties are not initialized (usually because the 
	 * <i>configuration.properties</i> was not found) 
	 */
	public static Location createOrOpenTDB() throws ConfigException {
		if (configProperties != null) {
			// String tdbAssemblerFile = 
			//	  configProperties.getProperty("tdb.assembler.location", DEFAULT_ASSEMBLER_FILE);
			
			String tdbStorageDirectory = 
				configProperties.getProperty("tdb.storage.directory", DEFAULT_STORAGE_DIRECTORY);
			
			TDB.init();
			TDBFactory.createDataset(tdbStorageDirectory);
			
			return new Location(tdbStorageDirectory);
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
			transitiveContextModel.getOntProperty(ContextAssertionVocabulary.ENTITY_RELATION_ASSERTION).listSubProperties();
		
		for (; relationPropIt.hasNext();) {
			OntProperty prop = relationPropIt.next();
			if (!SPINFactory.isAbstract(prop)) {
				ContextAssertionType assertionType = ContextAssertionUtil.getType(prop, transitiveContextModel);
				switch(assertionType) {
				case Static:
					assertionIndex.addStaticContextAssertion(new ContextAssertionInfo(assertionType, 2, prop));
					break;
				case Profiled:
					assertionIndex.addProfiledContextAssertion(new ContextAssertionInfo(assertionType, 2, prop));
					break;
				case Sensed:
					assertionIndex.addSensedContextAssertion(new ContextAssertionInfo(assertionType, 2, prop));
					break;
				case Derived:
					assertionIndex.addDerivedContextAssertion(new ContextAssertionInfo(assertionType, 2, prop));
					break;
				default:
					break;
				}
				
				assertionIndex.mapAssertionStorage(prop);
			}
		}
		
		// search all EntityDataAssertions
		ExtendedIterator<? extends OntProperty> dataPropIt = 
			transitiveContextModel.getOntProperty(ContextAssertionVocabulary.ENTITY_DATA_ASSERTION).listSubProperties();
		
		for (; dataPropIt.hasNext();) {
			OntProperty prop = dataPropIt.next();
			if (!SPINFactory.isAbstract(prop)) {
				ContextAssertionType assertionType = ContextAssertionUtil.getType(prop, transitiveContextModel);
				switch(assertionType) {
				case Static:
					assertionIndex.addStaticContextAssertion(new ContextAssertionInfo(assertionType, 2, prop));
					break;
				case Profiled:
					assertionIndex.addProfiledContextAssertion(new ContextAssertionInfo(assertionType, 2, prop));
					break;
				case Sensed:
					assertionIndex.addSensedContextAssertion(new ContextAssertionInfo(assertionType, 2, prop));
					break;
				case Derived:
					assertionIndex.addDerivedContextAssertion(new ContextAssertionInfo(assertionType, 2, prop));
					break;
				default:
					break;
				}
				
				assertionIndex.mapAssertionStorage(prop);
			}
		}

		// create stores for subclasses of UnaryContextAssertion 
		ExtendedIterator<OntClass> unaryClassIt = transitiveContextModel
			.getOntClass(ContextAssertionVocabulary.UNARY_CONTEXT_ASSERTION).listSubClasses();
		for (; unaryClassIt.hasNext();) {
			OntClass cls = unaryClassIt.next();
			if (!SPINFactory.isAbstract(cls)) {
				ContextAssertionType assertionType = ContextAssertionUtil.getType(cls, transitiveContextModel);
				switch(assertionType) {
				case Static:
					assertionIndex.addStaticContextAssertion(new ContextAssertionInfo(assertionType, 1, cls));
					break;
				case Profiled:
					assertionIndex.addProfiledContextAssertion(new ContextAssertionInfo(assertionType, 1, cls));
					break;
				case Sensed:
					assertionIndex.addSensedContextAssertion(new ContextAssertionInfo(assertionType, 1, cls));
					break;
				case Derived:
					assertionIndex.addDerivedContextAssertion(new ContextAssertionInfo(assertionType, 1, cls));
					break;
				default:
					break;
				}
				
				assertionIndex.mapAssertionStorage(cls);
			}
		}
		
		// and subclasses of NaryContextAssertion
		ExtendedIterator<OntClass> naryClassIt = transitiveContextModel
			.getOntClass(ContextAssertionVocabulary.NARY_CONTEXT_ASSERTION).listSubClasses();
		for (; naryClassIt.hasNext();) {
			OntClass cls = naryClassIt.next();
			if (!SPINFactory.isAbstract(cls)) {
				ContextAssertionType assertionType = ContextAssertionUtil.getType(cls, transitiveContextModel);
				switch(assertionType) {
				case Static:
					assertionIndex.addStaticContextAssertion(new ContextAssertionInfo(assertionType, 3, cls));
					break;
				case Profiled:
					assertionIndex.addProfiledContextAssertion(new ContextAssertionInfo(assertionType, 3, cls));
					break;
				case Sensed:
					assertionIndex.addSensedContextAssertion(new ContextAssertionInfo(assertionType, 3, cls));
					break;
				case Derived:
					assertionIndex.addDerivedContextAssertion(new ContextAssertionInfo(assertionType, 3, cls));
					break;
				default:
					break;
				}
				
				assertionIndex.mapAssertionStorage(cls);
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
	 * @param basicContextModel The ontology defining the context model, with no attached inference rules
	 * @return A {@link DerivationRuleDictionary} instance which contains maps of ContextEntity to 
	 * list of SPIN Rules and ContextAssertion to list of SPIN Rules.
	 */
	public static DerivationRuleDictionary buildAssertionDictionary(OntModel basicContextModel) {
		DerivationRuleDictionary dict = new DerivationRuleDictionary();
		
		/*
		 * Collect spin:deriveassertion rules in basicContextModel
		 */
		Property deriveAssertionProp = ResourceFactory.createProperty(SPIN.NS + "deriveassertion");
		Map<CommandWrapper, Map<String,RDFNode>> initialTemplateBindings = 
				new HashMap<CommandWrapper, Map<String,RDFNode>>();
		Map<Resource,List<CommandWrapper>> cls2Query = SPINQueryFinder.getClass2QueryMap(
				basicContextModel, basicContextModel, deriveAssertionProp, false, initialTemplateBindings, false);
		
		
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
				OntResource derivedAssertionResource = null; 
				
				OntModel contextBasicInfModel = getTransitiveInferenceModel(basicContextModel);
				ContextAssertionFinder ruleBodyFinder = new ContextAssertionFinder(whereElements, contextBasicInfModel);
				
				// run context assertion rule body finder and collect results
				ruleBodyFinder.run();
				List<ContextAssertion> bodyContextAssertions = ruleBodyFinder.getResult();
				
				// look through asserted triples as part of the CONSTRUCT for the derived assertion and retrieve
				// its resource type
				for (TripleTemplate tpl : constructedTriples) {
					if (tpl.getPredicate().equals(
						contextBasicInfModel.getProperty(ContextAssertionVocabulary.CONTEXT_ASSERTION_RESOURCE))) {
						derivedAssertionResource = contextBasicInfModel.getOntResource(tpl.getObjectResource());
					}
				}
				
				// there is only one head ContextAssertion - the derived one
				DerivedAssertionWrapper derivationWrapper = new DerivedAssertionWrapper(derivedAssertionResource, cmd);
				
				for (ContextAssertion assertion : bodyContextAssertions) {
					// System.out.println(assertion.getAssertionResource().getURI() + ": " + assertion.getAssertionType());
					dict.addDerivationForAssertion(assertion.getAssertionResource(), derivationWrapper);
				}
				
				// add all ContextEntity to SPIN:Rule list mappings
				dict.addCommandForEntity(res, derivationWrapper);
				
				// add reverse map from SPIN:Rule to ContextEntity
				dict.setEntityForDerivation(derivationWrapper, res);
			}
		}
		
		return dict;
	}
}
