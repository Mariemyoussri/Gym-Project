import React, { useState } from "react";
import { SliderData } from "./SliderData";
import FaArrowAltCircleLeft from "@mui/icons-material/ArrowCircleLeftRounded";
import FaArrowAltCircleRight from "@mui/icons-material/ArrowCircleRightRounded";
import Grid from "@mui/material/Grid";

const ImageSlider = ({ slides }) => {
  const [current, setCurrent] = useState(0);
  const length = slides.length;

  const nextSlide = () => {
    setCurrent(current === length - 1 ? 0 : current + 1);
  };

  const prevSlide = () => {
    setCurrent(current === 0 ? length - 1 : current - 1);
  };

  if (!Array.isArray(slides) || slides.length <= 0) {
    return null;
  }

  return (
    <Grid
      xs="auto"
      container
      direction="row"
      justifyContent="center"
      alignItems="center"
      spacing={1}
    >
      <Grid item xs="auto">
        <FaArrowAltCircleLeft
          className="left-arrow"
          onClick={prevSlide}
          sx={{ fontSize: 60, color: "white" }}
        />
      </Grid>
      <Grid item xs={6.5}>
        {SliderData.map((slide, index) => {
          return (
            <div
              className={index === current ? "slide active" : "slide"}
              key={index}
            >
              {index === current && (
                // eslint-disable-next-line jsx-a11y/img-redundant-alt
                <img
                  src={slide.src}
                  alt="travel image"
                  className="image"
                  sx={{ height: 10 }}
                />
              )}
            </div>
          );
        })}
      </Grid>
      <Grid item xs="auto">
        <FaArrowAltCircleRight
          className="right-arrow"
          onClick={nextSlide}
          sx={{ fontSize: 60, color: "white" }}
        />
      </Grid>
    </Grid>
  );
};

export default ImageSlider;