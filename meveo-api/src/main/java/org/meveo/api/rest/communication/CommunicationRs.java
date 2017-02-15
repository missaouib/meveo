package org.meveo.api.rest.communication;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.meveo.api.dto.ActionStatus;
import org.meveo.api.dto.communication.CommunicationRequestDto;
import org.meveo.api.rest.IBaseRs;

@Path("Communication")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })

public interface CommunicationRs extends IBaseRs {

    /**
     * Receives inbout communication from external source given the rest url above. MEVEO handles it by throwing an inbount communication event with the communicationRequestDto.
     * 
     * @param communicationRequestDto
     * @return
     */
    @POST
    @Path("/inbound")
    public ActionStatus inboundCommunication(CommunicationRequestDto communicationRequestDto);

}
