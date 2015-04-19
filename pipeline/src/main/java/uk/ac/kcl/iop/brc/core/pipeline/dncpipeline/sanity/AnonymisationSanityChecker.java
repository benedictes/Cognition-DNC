/*
        Copyright (c) 2015 King's College London

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	    http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/

package uk.ac.kcl.iop.brc.core.pipeline.dncpipeline.sanity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.kcl.iop.brc.core.pipeline.common.utils.TimeUtil;
import uk.ac.kcl.iop.brc.core.pipeline.dncpipeline.model.Patient;
import uk.ac.kcl.iop.brc.core.pipeline.dncpipeline.model.PatientAddress;
import uk.ac.kcl.iop.brc.core.pipeline.dncpipeline.service.anonymisation.AnonymisationService;

import javax.annotation.PostConstruct;
import java.text.ParseException;

import static junit.framework.TestCase.assertFalse;

@Component
public class AnonymisationSanityChecker {

    @Autowired
    private AnonymisationService anonymisationService;

    @PostConstruct
    public void checkBasicAnonymisationRules() throws ParseException {
        Patient patient = new Patient();
        patient.addForeName("TestName1");
        patient.addForeName("TestName2");
        patient.addSurname("TestLastName1");
        patient.addSurname("TestLastName1");
        patient.setNHSNumber("11122");
        PatientAddress patientAddress1 = new PatientAddress();
        patientAddress1.setAddress("addressText");
        patientAddress1.setPostCode("cb4 2za");
        PatientAddress patientAddress2 = new PatientAddress();
        patientAddress2.setAddress("addressText");
        patientAddress2.setPostCode("cb4 2za");
        patient.addAddress(patientAddress1);
        patient.addAddress(patientAddress2);
        patient.addPhoneNumber("50090051234");
        patient.addPhoneNumber("11090051234");
        patient.setDateOfBirth(TimeUtil.getDateFromString("09/05/1990", "dd/MM/yyyy"));

        String anonymisedText = anonymisationService.anonymisePatientPlainText(patient, "\n" +
                "\n" +
                "TestName1 TestName2 TestLastName1 TestLastName2\n" +
                "\n" +
                "\n" +
                "11122\n" +
                "50090051234" +
                "\n 09/05/1990" + "\n"
                + "cb42za");
        System.out.println(anonymisedText);
        if (anonymisedText.contains("TestName1") || anonymisedText.contains("TestName2")) {
            throw new AssertionError("First name pseudonymisation is not working! Please check config/anonymisation/nameRules");
        }
        if (anonymisedText.contains("TestLastName1") || anonymisedText.contains("TestLastName2")) {
            throw new AssertionError("Last name pseudonymisation is not working! Please check config/anonymisation/nameRules");
        }
        if (anonymisedText.contains("11122")) {
            throw new AssertionError("NHS Number pseudonymisation is not working! Please check config/anonymisation/nhsIdRules");
        }
        if (anonymisedText.contains("50090051234")) {
            throw new AssertionError("Phone number pseudonymisation is not working! Please check config/anonymisation/phoneRules");
        }
        if (anonymisedText.contains("09/05/1990")) {
            throw new AssertionError("Date of birth pseudonymisation is not working! Please check config/anonymisation/dateOfBirthRules");
        }
        if (anonymisedText.contains("cb42za")) {
            throw new AssertionError("Post code pseudonymisation is not working! Please check config/anonymisation/addressRules");
        }
    }

}