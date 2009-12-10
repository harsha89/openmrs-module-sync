/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.sync;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.ConceptAnswer;
import org.openmrs.ConceptDescription;
import org.openmrs.ConceptName;
import org.openmrs.ConceptNameTag;
import org.openmrs.ConceptNumeric;
import org.openmrs.ConceptSet;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.test.TestUtil;
import org.springframework.test.annotation.NotTransactional;

/**
 *
 */
public class SyncConceptTest extends SyncBaseTest {
	
	@Override
	public String getInitialDataset() {
		return "org/openmrs/module/sync/include/SyncCreateTest.xml";
	}
	
	@Test
	@NotTransactional
	public void shouldSaveConceptDescriptionsWithConcept() throws Exception {
		runSyncTest(new SyncTestHelper() {
			
			String conceptName = "A concept";
			
			public void runOnChild() throws Exception {
				ConceptService cs = Context.getConceptService();
				
				TestUtil.printOutTableContents(getConnection(), "concept", "concept_name");
				
				Concept concept = new Concept();
				concept.setDatatype(cs.getConceptDatatypeByName("Coded"));
				concept.setConceptClass(cs.getConceptClassByName("Question"));
				concept.addName(new ConceptName(conceptName, Context.getLocale()));
				concept.addDescription(new ConceptDescription("asdf", Context.getLocale()));
				cs.saveConcept(concept);
				
				TestUtil.printOutTableContents(getConnection(), "concept", "concept_name");
			}
			
			public void runOnParent() throws Exception {
				ConceptService cs = Context.getConceptService();
				
				Context.setLocale(Locale.UK);
				log.error("The current locale: " + Context.getLocale());
				
				TestUtil.printOutTableContents(getConnection(), "concept", "concept_name");
				
				Concept c = cs.getConceptByName(conceptName);
				assertNotNull("Failed to create the concept", c);
				
				log.info("descriptions: " + c.getDescriptions());
				assertTrue("Failed to transfer descriptions", c.getDescriptions().size() > 0);
			}
		});
	}
	
	@Test
	@NotTransactional
	public void shouldSaveConceptCoded() throws Exception {
		runSyncTest(new SyncTestHelper() {
			
			public void runOnChild() {
				ConceptService cs = Context.getConceptService();
				
				Concept coded = new Concept();
				coded.setDatatype(cs.getConceptDatatypeByName("Coded"));
				coded.setConceptClass(cs.getConceptClassByName("Question"));
				coded.setSet(false);
				ConceptName name = new ConceptName("CODED", Context.getLocale());
				ConceptNameTag tag = new ConceptNameTag("default", "The default name");
				tag.setConceptNameTagId(1);
				name.addTag(tag);
				coded.addName(name);
				coded.addDescription(new ConceptDescription("asdf", Context.getLocale()));
				coded.addAnswer(new ConceptAnswer(cs.getConceptByName("OTHER NON-CODED")));
				coded.addAnswer(new ConceptAnswer(cs.getConceptByName("NONE")));
				cs.saveConcept(coded);
			}
			
			public void runOnParent() {
				Context.clearSession();
				
				ConceptService cs = Context.getConceptService();
				
				log.error("The current locale: " + Context.getLocale());
				
				Concept c = cs.getConceptByName("CODED");
				assertNotNull("Failed to create CODED concept", c);
				
				log.info("names: " + c.getNames(true));
				try {
					Field field = Concept.class.getDeclaredField("names");
					field.setAccessible(true);
					log.warn("concept names list via reflection: " + field.get(c));
				}
				catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					log.error("Error generated", e);
				}
				catch (SecurityException e) {
					// TODO Auto-generated catch block
					log.error("Error generated", e);
				}
				catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					log.error("Error generated", e);
				}
				catch (NoSuchFieldException e) {
					// TODO Auto-generated catch block
					log.error("Error generated", e);
				}
				assertTrue("Failed to transfer names", c.getNames(true).size() > 0);
				assertEquals(c.getConceptClass().getConceptClassId(), cs.getConceptClassByName("Question")
				        .getConceptClassId());
				assertEquals(c.getDatatype().getConceptDatatypeId(), cs.getConceptDatatypeByName("Coded")
				        .getConceptDatatypeId());
				
				Collection<ConceptAnswer> answers = c.getAnswers();
				assertEquals(2, answers.size());
				
			}
		});
	}
	
	@Test
	@NotTransactional
	public void shouldSaveConceptNumeric() throws Exception {
		runSyncTest(new SyncTestHelper() {
			
			ConceptService cs;
			
			public void runOnChild() {
				cs = Context.getConceptService();
				ConceptNumeric cn = new ConceptNumeric();
				cn.addName(new ConceptName("SOMETHING NUMERIC", Context.getLocale()));
				cn.setDatatype(cs.getConceptDatatypeByName("Numeric"));
				cn.setConceptClass(cs.getConceptClassByName("Question"));
				cn.setSet(false);
				cn.setPrecise(true);
				cn.setLowAbsolute(0d);
				cn.setHiCritical(100d);
				cs.saveConcept(cn);
			}
			
			public void runOnParent() {
				Context.clearSession();
				
				Concept c = cs.getConceptByName("SOMETHING NUMERIC");
				assertNotNull("Failed to create numeric", c);
				System.out.println("1: " + c);
				assertNotNull("No names got transferred", c.getNames());
				assertTrue("No names got transferred", c.getNames().size() > 0);
				assertEquals(c.getName().getName(), "SOMETHING NUMERIC");
				ConceptNumeric cn = cs.getConceptNumeric(c.getConceptId());
				assertEquals("Concept numeric absolute low values do not match", (Double) 0d, cn.getLowAbsolute());
				assertEquals("Concept nuermic high critical values do not match", (Double) 100d, cn.getHiCritical());
				assertEquals("Concept numeric datatypes does not match", "Numeric", cn.getDatatype().getName());
				assertEquals("Concept numeric classes does not match", "Question", cn.getConceptClass().getName());
				
			}
		});
	}
	
	@Test
	@NotTransactional
	public void shouldSaveConceptSet() throws Exception {
		runSyncTest(new SyncTestHelper() {
			
			ConceptService cs;
			
			private int conceptNumericId = 99997;
			
			private int conceptCodedId = 99998;
			
			private int conceptSetId = 99999;
			
			public void runOnChild() {
				cs = Context.getConceptService();
				
				ConceptNumeric cn = new ConceptNumeric();
				cn.setConceptId(conceptNumericId);
				cn.addName(new ConceptName("SOMETHING NUMERIC", Context.getLocale()));
				cn.setDatatype(cs.getConceptDatatypeByName("Numeric"));
				cn.setConceptClass(cs.getConceptClassByName("Question"));
				cn.setSet(false);
				cn.setPrecise(true);
				cn.setLowAbsolute(0d);
				cn.setHiCritical(100d);
				cs.saveConcept(cn);
				
				Concept coded = new Concept();
				coded.setConceptId(conceptCodedId);
				coded.addName(new ConceptName("SOMETHING CODED", Context.getLocale()));
				coded.setDatatype(cs.getConceptDatatypeByName("Coded"));
				coded.setConceptClass(cs.getConceptClassByName("Question"));
				coded.setSet(false);
				
				Concept other = cs.getConceptByName("OTHER NON-CODED");
				assertNotNull("Failed to get concept OTHER NON-CODED", other);
				
				Concept none = cs.getConceptByName("NONE");
				assertNotNull("Failed to get concept NONE", none);
				
				coded.addAnswer(new ConceptAnswer(other));
				coded.addAnswer(new ConceptAnswer(none));
				coded.addAnswer(new ConceptAnswer(cn));
				cs.saveConcept(coded);
				
				//ConceptSet conceptSet = new ConceptSet();
				
				Concept set = new Concept(conceptSetId);
				
				log.info("Locale: " + Context.getLocale());
				
				set.addName(new ConceptName("A CONCEPT SET", Context.getLocale()));
				set.setDatatype(cs.getConceptDatatypeByName("N/A"));
				set.setConceptClass(cs.getConceptClassByName("ConvSet"));
				set.setSet(true);
				Set<ConceptSet> cset = new HashSet<ConceptSet>();
				cset.add(new ConceptSet(coded, 1d));
				cset.add(new ConceptSet(cn, 2d));
				set.setConceptSets(cset);
				cs.saveConcept(set);
			}
			
			public void runOnParent() {
				Context.clearSession();
				
				Concept c = cs.getConceptByName("SOMETHING NUMERIC");
				assertNotNull("Failed to create numeric", c);
				
				assertNotNull("Failed to find a name", c.getName());
				assertEquals("Concept names do not match", "SOMETHING NUMERIC", c.getName().getName());
				
				ConceptNumeric cn = cs.getConceptNumeric(c.getConceptId());
				assertEquals("Concept numeric absolute low values do not match", (Double) 0d, cn.getLowAbsolute());
				assertEquals("Concept numeric critical high values do not match", (Double) 100d, cn.getHiCritical());
				assertEquals("Concept numeric datatypes do not match", "Numeric", cn.getDatatype().getName());
				assertEquals("Concept numeric classes do not match", "Question", cn.getConceptClass().getName());
				
				// Test the coded concept 			
				Concept conceptCoded = cs.getConceptByName("SOMETHING CODED");
				assertNotNull("Failed to save coded concept - Could not retrieve concept by name", conceptCoded);
				
				Set<String> answers = new HashSet<String>();
				for (ConceptAnswer a : conceptCoded.getAnswers()) {
					answers.add(a.getAnswerConcept().getName().getName());
				}
				Assert.assertTrue(answers.contains("SOMETHING NUMERIC"));
				Assert.assertTrue(answers.contains("OTHER NON-CODED"));
				Assert.assertTrue(answers.contains("NONE"));
				
				// Test the concept set 
				Concept conceptSet = cs.getConceptByName("A CONCEPT SET");
				assertNotNull("Failed to create coded concept - Could not retrieve code concept by name", conceptSet);
				
				assertEquals("Failed to create concept set - Concept set should have two elements", conceptSet
				        .getConceptSets().size(), 2);
				
			}
		});
	}
	
	@Test
	@NotTransactional
	public void shouldEditConcepts() throws Exception {
		runSyncTest(new SyncTestHelper() {
			
			ConceptService cs;
			
			int numAnswersBefore;
			
			public void runOnChild() {
				cs = Context.getConceptService();
				Concept wt = cs.getConceptByName("WEIGHT");
				ConceptNumeric weight = cs.getConceptNumeric(wt.getConceptId());
				weight.setHiCritical(200d);
				cs.saveConcept(weight);
				
				Concept coded = cs.getConceptByName("CAUSE OF DEATH");
				assertNotNull(coded);
				Concept malaria = new Concept(99999);
				malaria.addName(new ConceptName("MALARIA", Context.getLocale()));
				malaria.setDatatype(cs.getConceptDatatypeByName("N/A"));
				malaria.setConceptClass(cs.getConceptClassByName("Diagnosis"));
				cs.saveConcept(malaria);
				numAnswersBefore = coded.getAnswers().size();
				coded.addAnswer(new ConceptAnswer(malaria));
				cs.saveConcept(coded);
			}
			
			public void runOnParent() {
				Concept wt = cs.getConceptByName("WEIGHT");
				ConceptNumeric weight = cs.getConceptNumeric(wt.getConceptId());
				assertEquals("Failed to change property on a numeric concept", (Double) 200d, weight.getHiCritical());
				
				Concept malaria = cs.getConceptByName("MALARIA");
				assertNotNull("Implicit create of concept referenced in answer failed", malaria);
				
				Concept coded = cs.getConceptByName("CAUSE OF DEATH");
				assertEquals("Adding answer failed", numAnswersBefore + 1, coded.getAnswers().size());
			}
		});
	}
	
	@Test
	@NotTransactional
	public void shouldAddNameToConcept() throws Exception {
		runSyncTest(new SyncTestHelper() {
			
			ConceptService cs = Context.getConceptService();
			
			int numNamesBefore;
			
			public void runOnChild() {
				Concept wt = cs.getConceptByName("WEIGHT");
				numNamesBefore = wt.getNames().size();
				wt.addName(new ConceptName("POIDS", Locale.FRENCH));
				cs.saveConcept(wt);
			}
			
			public void runOnParent() {
				Concept wt = cs.getConceptByName("WEIGHT");
				assertNotNull(wt);
				assertEquals("Should be one more name than before", numNamesBefore + 1, wt.getNames().size());
				assertEquals("Incorrect french name", wt.getName(Locale.FRENCH).getName(), "POIDS");
			}
		});
	}
	
	@Test
	@NotTransactional
	public void shouldAddDescriptionToConcept() throws Exception {
		runSyncTest(new SyncTestHelper() {
			
			ConceptService cs = Context.getConceptService();
			
			int numDescriptionsBefore;
			
			public void runOnChild() {
				Concept wt = cs.getConceptByName("WEIGHT");
				numDescriptionsBefore = wt.getDescriptions().size();
				wt.addDescription(new ConceptDescription("Everyone tries to lose this", Locale.FRENCH));
				cs.saveConcept(wt);
			}
			
			public void runOnParent() {
				Concept wt = cs.getConceptByName("WEIGHT");
				assertNotNull(wt);
				assertEquals("Should be one more description than before", numDescriptionsBefore + 1, wt.getDescriptions()
				        .size());
				assertEquals("Incorrect french description", wt.getDescription(Locale.FRENCH).getDescription(),
				    "Everyone tries to lose this");
			}
		});
	}
	
	@Test
	@NotTransactional
	public void shouldAddTagToConceptName() throws Exception {
		runSyncTest(new SyncTestHelper() {
			
			ConceptService cs = Context.getConceptService();
			
			int numTagsBefore;
			
			public void runOnChild() {
				Concept wt = cs.getConceptByName("WEIGHT");
				ConceptName cn = wt.getName();
				numTagsBefore = cn.getTags().size();
				ConceptNameTag tag = cs.getConceptNameTagByName("default");
				cn.addTag(tag);
				cs.saveConcept(wt);
			}
			
			public void runOnParent() {
				Concept wt = cs.getConceptByName("WEIGHT");
				assertNotNull(wt);
				ConceptName cn = wt.getName();
				assertEquals("Should be one more tag than before", numTagsBefore + 1, cn.getTags().size());
				assertEquals("tag not added", true, cn.hasTag("default"));
			}
		});
	}
}