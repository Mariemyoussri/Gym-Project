import * as React from "react";
import { useState, useEffect } from "react";
import Chip from "@mui/material/Chip";
import FitnessCenterIcon from "@mui/icons-material/FitnessCenter";
import Accordion from "@mui/material/Accordion";
import AccordionSummary from "@mui/material/AccordionSummary";
import AccordionDetails from "@mui/material/AccordionDetails";
import Typography from "@mui/material/Typography";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
//import Data from "./Data";
import Stack from "@mui/material/Stack";
import {ReactSession} from 'react-client-session';
import IconButton from "@mui/material/IconButton";
import DeleteIcon from "@mui/icons-material/Delete";
import {removePrograms,getPrograms} from "../../api/ProfileApi";
import Alert from "@mui/material/Alert";

export default function UserPrograms({Programs}) {
  const [programs, setPrograms] = useState(Programs);
  const [error, setError] = React.useState(0);
  const [errorMessage, setErrorMessage] = React.useState("");

  const handleRemove = (program) => {
    var values = {
      userId: ReactSession.get("user").Id,
      role: ReactSession.get("user").role,
      statusCode: 0,
    };
    removePrograms(values,ReactSession.get("user").userName,program.programId).then((data) => {
      
    });
    getPrograms(values, ReactSession.get("user").userName).then((program) => {
      console.log(program);
      setPrograms(program);
    });
  };
  return (
    <div>
      {programs.map((program) => (
        <Accordion>
          <AccordionSummary
            expandIcon={<ExpandMoreIcon />}
            aria-controls="panel1a-content"
            id="panel1a-header"
          >
            <IconButton onClick={()=>handleRemove(program)} style={{ color: "#cc1b85" }} aria-label="add an alarm">
            <DeleteIcon />
          </IconButton>
            <Typography>{program.title}</Typography>
          </AccordionSummary>
          <AccordionDetails>
            <Typography>
              {/* {program.content} */}

              <Stack spacing={1} alignItems="center">
                {program.classesFollowUp.map((content) => (
                  <Stack direction="row" spacing={1}>
                    <Chip
                      color="secondary"
                      label={content.className}
                      icon={<FitnessCenterIcon />}
                    />
                    <Chip label={content.attendedSessions} color="success" />
                  </Stack>
                ))}
              </Stack>
            </Typography>
          </AccordionDetails>
        </Accordion>
      ))}
    </div>
  );
}
