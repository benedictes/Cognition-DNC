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

package uk.ac.kcl.iop.brc.core.pipeline.dncpipeline.service;

import com.google.gson.Gson;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import spark.Request;
import uk.ac.kcl.iop.brc.core.pipeline.dncpipeline.data.CoordinatesDao;
import uk.ac.kcl.iop.brc.core.pipeline.dncpipeline.model.DNCWorkCoordinate;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

import static spark.Spark.get;

@Service
public class CoordinatorService {

    private static Logger logger = Logger.getLogger(CoordinatorService.class);
    public static String NO_COORDINATE_LEFT = "NO_COORDINATE_TO_PROCESS";

    @Autowired
    private CoordinatesDao coordinatesDao;

    private List<DNCWorkCoordinate> allCoordinates;

    private long lastCheckpoint = 0;

    private int chunkSize = 200;

    /**
     * Starts serving coordinates to clients who request work.
     */
    public void startServer() {
        logger.info("Loading coordinates from DB.");
        allCoordinates = coordinatesDao.getCoordinates();
        if (CollectionUtils.isEmpty(allCoordinates)) {
            logger.warn("No coordinates to process! Exiting!");
            return;
        }
        get("/", (request, response) -> handleRequest(request));
    }

    private synchronized String handleRequest(Request request) {
        if (! relevantRequest(request)) {
            return "";
        }
        String cognitionName = request.headers("CognitionName");
        logger.info("Handling request from " + cognitionName + " " + request.ip());

        List<DNCWorkCoordinate> workLoad = getChunkOfList(allCoordinates, lastCheckpoint, chunkSize);
        if (CollectionUtils.isEmpty(workLoad)) {
            return NO_COORDINATE_LEFT;
        }
        long newCheckPoint = lastCheckpoint + chunkSize;
        logger.info("Serving from " + lastCheckpoint + " to " + newCheckPoint + " to " + cognitionName + " " + request.ip());
        lastCheckpoint = newCheckPoint;

        return new Gson().toJson(workLoad);
    }

    private boolean relevantRequest(Request request) {
        if (request == null) {
            return false;
        }
        String dncRequest = request.headers("DNCRequest");

        return !StringUtils.isBlank(dncRequest) && "true".equalsIgnoreCase(dncRequest);
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public List<DNCWorkCoordinate> getChunkOfList(List<DNCWorkCoordinate> coordinateList, long start, int size) {
        return coordinateList.stream().skip(start).limit(size).collect(Collectors.toList());
    }
}
