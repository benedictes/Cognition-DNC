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

package uk.ac.kcl.iop.brc.core.pipeline.dncpipeline.data;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.transform.Transformers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import uk.ac.kcl.iop.brc.core.pipeline.common.data.BaseDao;
import uk.ac.kcl.iop.brc.core.pipeline.dncpipeline.data.helper.ClobHelper;
import uk.ac.kcl.iop.brc.core.pipeline.dncpipeline.model.Patient;
import uk.ac.kcl.iop.brc.core.pipeline.dncpipeline.model.PatientAddress;
import uk.ac.kcl.iop.brc.core.pipeline.dncpipeline.model.PatientCarer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Repository
public class PatientDao extends BaseDao {

    @Autowired
    private ClobHelper clobHelper;

    private static Logger logger = Logger.getLogger(PatientDao.class);

    /**
     * Returns the patient with the given id.
     * Returns the patient from the cache if it's been fetched once.
     *
     * @param id
     * @return a patient object of the patient with the given id.
     */
    @Cacheable(value = "patients", key = "#id")
    public Patient getPatient(Long id) {
        Query getPatient = getCurrentSourceSession().getNamedQuery("getPatient");
        getPatient.setParameter("patientId", id);
        logger.info("Loading patient " + id);
        Patient patient = (Patient) getPatient.
                setFirstResult(0).setMaxResults(1).
                setResultTransformer(Transformers.aliasToBean(Patient.class)).uniqueResult();

        setNames(patient);
        setPhoneNumbers(patient);
        setAddresses(patient);
        setCarers(patient);
        setNhsNumbers(patient);
        setDateOfBirths(patient);

        return patient;
    }

    private void setNhsNumbers(Patient patient) {
        try {
            Query getNhsNumbers = getCurrentSourceSession().getNamedQuery("getNhsNumbers");
            getNhsNumbers.setParameter("patientId", patient.getId());
            List nhsNumbers = getNhsNumbers.list();
            nhsNumbers.forEach(object -> {
                if (object instanceof String) {
                    patient.addNhsNumber((String) object);
                } else {
                    Object[] nhsNumberRow = (Object[]) object;
                    String number = clobHelper.getStringFromExpectedClob(nhsNumberRow[0]);
                    patient.addNhsNumber(number);
                }
            });
        } catch (Exception ex) {
            logger.warn("Error while loading NHS Numbers of patient " + patient.getId() + ". Does the specified table exist? " + ex.getMessage());
        }
    }

    private void setDateOfBirths(Patient patient) {
        try {
            Query getDateOfBirths = getCurrentSourceSession().getNamedQuery("getDateOfBirths");
            getDateOfBirths.setParameter("patientId", patient.getId());
            List dateOfBirths = getDateOfBirths.list();
            dateOfBirths.forEach(object -> {
                if (object instanceof Date) {
                    patient.addDateOfBirth((Date) object);
                } else {
                    Object[] dateOfBirthRow = (Object[]) object;
                    Date dateOfBirth = (Date) dateOfBirthRow[0];
                    patient.addDateOfBirth(dateOfBirth);
                }
            });
        } catch (Exception ex) {
            logger.warn("Error while loading date of births of patient " + patient.getId() + ". Does the specified table exist? " + ex.getMessage());
        }
    }

    private void setCarers(Patient patient) {
        try {
            Query getCarers = getCurrentSourceSession().getNamedQuery("getCarers");
            getCarers.setParameter("patientId", patient.getId());
            List carerObjects = getCarers.list();
            carerObjects.forEach(object -> {
                Object[] carerRow = (Object[]) object;
                String name = clobHelper.getStringFromExpectedClob(carerRow[0]);
                String lastName = clobHelper.getStringFromExpectedClob(carerRow[1]);
                PatientCarer carer = new PatientCarer(name, lastName);
                patient.addCarer(carer);
            });
        } catch (Exception ex) {
            logger.warn("Error while loading carers of patient " + patient.getId() + ". Does the specified carer table exist? " + ex.getMessage());
        }
    }

    private void setPhoneNumbers(Patient patient) {
        Query getPhoneNumbers = getCurrentSourceSession().getNamedQuery("getPhoneNumbers");
        getPhoneNumbers.setParameter("patientId", patient.getId());
        List list = getPhoneNumbers.list();
        List<String> phoneNumbers = new ArrayList<>();
        list.forEach(object -> {
            String phone = clobHelper.getStringFromExpectedClob(object);
            phoneNumbers.add(phone);
        });
        patient.setPhoneNumbers(phoneNumbers);
    }

    /**
     * Fetches the addresses of the patient from the DB and assigns them to the given patient.
     *
     * @param patient
     */
    private void setAddresses(Patient patient) {
        try {
            Query getAddresses = getCurrentSourceSession().getNamedQuery("getAddresses");
            getAddresses.setParameter("patientId", patient.getId());
            List addressObjects = getAddresses.list();
            addressObjects.forEach(object -> {
                Object[] addressRow = (Object[]) object;
                String addressStr = clobHelper.getStringFromExpectedClob(addressRow[0]);
                String postCode = "";
                if (addressRow.length > 1) {
                    postCode = clobHelper.getStringFromExpectedClob(addressRow[1]);
                }
                PatientAddress address = PatientAddress.newAddressPostCode(addressStr, postCode);
                patient.addAddress(address);
            });
        } catch (Exception ex) {
            logger.warn("Error while loading addresses of patient " + patient.getId() + ". Does the specified address table exist? " + ex.getMessage());
        }
    }

    /**
     * Fetches the names of the patient from the DB and assigns them to the given patient.
     *
     * @param patient
     */
    private void setNames(Patient patient) {
        try {
            Query getForeNames = getCurrentSourceSession().getNamedQuery("getPatientNames");
            getForeNames.setParameter("patientId", patient.getId());
            List foreNames = getForeNames.list();
            if (CollectionUtils.isEmpty(foreNames)) {
                logger.warn("!! No name/surname was found for patient with id " + patient.getId());
                return;
            }
            for (Object dbObject : foreNames) {
                Object[] namePair = (Object[]) dbObject;
                String name = clobHelper.getStringFromExpectedClob(namePair[0]);
                String lastName = clobHelper.getStringFromExpectedClob(namePair[1]);
                patient.addForeName(name);
                patient.addSurname(lastName);
            }
        } catch (Exception ex) {
            logger.warn("Error while loading names of patient " + patient.getId() + ". Does the specified address table exist? " + ex.getMessage());
        }
    }

}
