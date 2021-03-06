package com.example.gymserver.services;

import com.example.gymserver.dto.ProgramFollowUpDTO;
import com.example.gymserver.dto.SessionDTO;
import com.example.gymserver.dto.UserIdDTO;
import com.example.gymserver.mappers.PClassFollowUpMapper;
import com.example.gymserver.mappers.SessionMapper;
import com.example.gymserver.models.*;
import com.example.gymserver.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TraineeService {
    private AuthenticationService authenticationService;
    private ProgramRepository programRepository;
    private PClassFollowUpRepository pClassFollowUpRepository;
    private PClassDetailsRepository pClassDetailsRepository;
    private TraineeRepository traineeRepository;
    private SessionRepository sessionRepository;


    public static final int NO_BOOKED_PROGRAM_STATUS_CODE = -1;
    public static final int NO_REMAINING_SESSIONS_STATUS_CODE = -2;
    public static final int TRAINEE_REGISTERED_BEFORE_STATUS_CODE = -4;
    public static final int FULL_SESSION_STATUS_CODE = -5;
    public static final int INVALID_ENTITY_STATUS_CODE = -10;
    public static final int INVALID_DELETE_STATUS_CODE = -1;
    public static final int SUCCESS_STATUS_CODE = 0;



    @Autowired
    public TraineeService(AuthenticationService authenticationService,
                          ProgramRepository programRepository,
                          PClassFollowUpRepository pClassFollowUpRepository,
                          PClassDetailsRepository pClassDetailsRepository,
                          TraineeRepository traineeRepository,
                          SessionRepository sessionRepository){
        this.authenticationService = authenticationService;
        this.pClassFollowUpRepository = pClassFollowUpRepository;
        this.programRepository = programRepository;
        this.pClassDetailsRepository = pClassDetailsRepository;
        this.traineeRepository = traineeRepository;
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public List<SessionDTO> getSessions(String userName, UserIdDTO userIdDTO) {
        if(!this.authenticationService.authenticateUser(userIdDTO.getUserId(), userName))
            return null;
        else{
            List<SessionDTO> sessionDTOS = new ArrayList<>();
            Trainee trainee = this.traineeRepository.getById(userIdDTO.getUserId());
            deletePastSessions(trainee);
            for(Session session : trainee.getSessions()){
                sessionDTOS.add(SessionMapper.toSessionDTO(session));
            }
            return sessionDTOS;
        }
    }


    public void deletePastSessions(Trainee trainee){
        trainee.getSessions().removeIf(session -> session.getEndTime().isBefore(LocalDateTime.now()));
    }

    @Transactional
    public List<ProgramFollowUpDTO> getFollowUps(String userName, UserIdDTO userIdDTO) {
        if(!this.authenticationService.authenticateUser(userIdDTO.getUserId(), userName))
            return null;
        else{
            List<ProgramFollowUpDTO> traineeFollowUp = new ArrayList<>();
            List<Long> programsId = this.pClassFollowUpRepository
                    .findProgramsIdByTraineeId(userIdDTO.getUserId()).orElse(null);
            if(programsId != null && programsId.size() > 0){
                for(Long id : programsId){
                    List<PClassFollowUp> classesFollowUp = this.pClassFollowUpRepository
                            .findFollowUpsByTraineeAndProgram(userIdDTO.getUserId(), id).orElse(null);
                    if(classesFollowUp != null && classesFollowUp.size() > 0){
                        if( ! deleteProgramIfExpired(id,classesFollowUp.get(0).getEndDate()))
                            traineeFollowUp.add(PClassFollowUpMapper.toProgramFollowUpDTO(classesFollowUp));
                    }else System.out.println(id + " is NULL!!!!");
                }
            }else System.out.println("ERROR IN GETTING PROGRAM IDS");
            return traineeFollowUp;
        }
    }


    public boolean deleteProgramIfExpired(long programID, LocalDate endDate){
        if( endDate.isBefore(LocalDate.now())){
            pClassFollowUpRepository.deleteProgram(programID);
            return true;
        }
        return false;
    }

    public int bookProgram(String userName, Long programID, UserIdDTO userIdDTO) {
        int statusCode = 0;
        if(!this.authenticationService.authenticateUser(userIdDTO.getUserId(), userName))
            statusCode = AuthenticationService.UNAUTHENTICATED_USER_STATUS_CODE;
        else{
            // Check if program reserved before
            List<PClassFollowUp> classesFollowUp = this.pClassFollowUpRepository
                    .findFollowUpsByTraineeAndProgram(userIdDTO.getUserId(), programID).orElse(null);
            System.out.println(programID + " PROGRAM ID !");
            System.out.println(classesFollowUp.size());
            if( classesFollowUp != null && classesFollowUp.size() > 0)
                return TRAINEE_REGISTERED_BEFORE_STATUS_CODE;

            Program program = this.programRepository.findById(programID).orElse(null);
            if(program != null){
                List<PClassDetails> programDetails = this.pClassDetailsRepository
                        .findDetailsByProgramId(programID).orElse(null);
                if(programDetails != null && programDetails.size() != 0){
                    Trainee trainee = this.traineeRepository.findById(userIdDTO.getUserId()).orElse(null);
                    for(PClassDetails classDetails : programDetails){
                        PClassFollowUp followUp = PClassFollowUp.builder()
                                                    .id(PClassFollowUpKey.builder()
                                                            .programId(program.getId())
                                                            .classId(classDetails.getId().getClassId())
                                                            .traineeId(trainee.getId())
                                                            .build())
                                                    .program(program)
                                                    .trainee(trainee)
                                                    .programClass(classDetails.getProgramClass())
                                                    .endDate(LocalDate.now().plusMonths(program.getDuration()))
                                                    .sessionsRemaining(classDetails.getNoOfClasses())
                                                    .used(false)
                                                    .build();
                        this.pClassFollowUpRepository.save(followUp);
                    }
                }
                else statusCode = INVALID_ENTITY_STATUS_CODE;
            }
            else statusCode = INVALID_ENTITY_STATUS_CODE;
        }
        return statusCode;
    }

    @Transactional
    public int bookSession(String userName, Long sessionID, UserIdDTO userIdDTO) {
        int statusCode = 0;
        if(!this.authenticationService.authenticateUser(userIdDTO.getUserId(), userName))
            statusCode = AuthenticationService.UNAUTHENTICATED_USER_STATUS_CODE;
        else{
            Trainee trainee = traineeRepository.getById(userIdDTO.getUserId());
            Session session = this.sessionRepository.findById(sessionID).orElse(null);
            if(!trainee.getSessions().contains(session)){
                if( session != null ){
                    List<PClassFollowUp> classFollowUps = pClassFollowUpRepository
                            .findFollowUpsByTraineeAndClass(userIdDTO.getUserId(),session.getProgramClass().getId()).orElse(null);
                    if( classFollowUps != null && classFollowUps.size() != 0){
                        boolean noRemainingSessions = true;
                        for(PClassFollowUp classFollowUp : classFollowUps){
                            if( classFollowUp.getSessionsRemaining() != 0 ){
                                noRemainingSessions = false;
                                if( !session.isFull() ){
                                    session.addAttendee();
                                    trainee.getSessions().add(session);
                                    classFollowUp.reserveSession();
                                }
                                else
                                    statusCode = FULL_SESSION_STATUS_CODE;
                                break;
                            }
                        }
                        if( noRemainingSessions )
                            statusCode = NO_REMAINING_SESSIONS_STATUS_CODE;
                    }
                    else
                        statusCode = NO_BOOKED_PROGRAM_STATUS_CODE;
                }
                else
                    statusCode = INVALID_ENTITY_STATUS_CODE;
            }
            else statusCode = TRAINEE_REGISTERED_BEFORE_STATUS_CODE;
        }
        return statusCode;
    }

    @Transactional
    public int deleteProgram(String userName, Long programID, UserIdDTO userIdDTO) {
        int statusCode = 0;
        if(!this.authenticationService.authenticateUser(userIdDTO.getUserId(), userName))
            statusCode = AuthenticationService.UNAUTHENTICATED_USER_STATUS_CODE;
        else{
            List<PClassFollowUp> classFollowUps = pClassFollowUpRepository
                    .findFollowUpsByTraineeAndProgram(userIdDTO.getUserId(), programID).orElse(null);
            if(classFollowUps != null && classFollowUps.size() != 0){
                for(PClassFollowUp pClassFollowUp: classFollowUps){
                    if(pClassFollowUp.isUsed())
                        return INVALID_DELETE_STATUS_CODE;
                }

                pClassFollowUpRepository.deleteProgram(programID);
                statusCode = SUCCESS_STATUS_CODE;
            }else
                statusCode = INVALID_ENTITY_STATUS_CODE;
        }
        return statusCode;
    }


    @Transactional
    public int deleteSession(String userName, Long sessionID, UserIdDTO userIdDTO) {
        int statusCode = 0;
        if(!this.authenticationService.authenticateUser(userIdDTO.getUserId(), userName))
            statusCode = AuthenticationService.UNAUTHENTICATED_USER_STATUS_CODE;
        else{
            Session session = this.sessionRepository.findById(sessionID).orElse(null);
            if(session != null){
                List<PClassFollowUp> pClassFollowUp = this.pClassFollowUpRepository.
                        findFollowUpsByTraineeAndClass(userIdDTO.getUserId(), session.getProgramClass().getId()).orElse(null);
                if(pClassFollowUp != null && pClassFollowUp.size() != 0){
                    for (PClassFollowUp pClassFollowUpTemp : pClassFollowUp) {
                        PClassDetails pClassDetails = this.pClassDetailsRepository
                                .findDetailsByProgramIdAndClassId(pClassFollowUpTemp.getProgram().getId(),
                                        pClassFollowUpTemp.getProgramClass().getId()).orElse(null);
                        if (pClassDetails != null) {
                            if (pClassFollowUpTemp.getSessionsRemaining() < pClassDetails.getNoOfClasses()) {
                                pClassFollowUpTemp.unreserveSession();
                                session.removeAttendee();
                                Trainee trainee = traineeRepository.getById(userIdDTO.getUserId());
                                trainee.getSessions().remove(session);
                                break;
                            }
                        } else
                            statusCode = INVALID_ENTITY_STATUS_CODE;
                    }
                }else
                    statusCode = INVALID_ENTITY_STATUS_CODE;
            }
            else
                statusCode = INVALID_ENTITY_STATUS_CODE;
        }
        return statusCode;
    }
}
