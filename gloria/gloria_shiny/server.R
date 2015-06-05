shinyServer(
  
  function(input, output) {
    
    #------------------------------------------------------
    # LOAD THE USER DATA
    
    Seg <- reactive({  
      inFile <- input$seg_file   
      if (!is.null(inFile)){
        data <- read.csv(inFile$datapath)  
        return(data)
      }
      
    }) 
    
    Rep <- reactive({  
      inFile <- input$rep_file   
      if (!is.null(inFile)){
        data <- read.csv(inFile$datapath)  
        return(data)
      }
      
    })      
    
    #------------------------------------------------------
    # Display the root image
    # Send a pre-rendered image, and don't delete the image after sending it
    

    

    
    
    #------------------------------------------------------
    # Plot the estimator distribution
    
    output$distPlot <- renderPlot({      
      
      
      if (is.null(input$seg_file) || is.null(input$rep_file)) { return()}
      
      
      # Load the reporter data
      reporter <- Rep()
      
      # Load the segment data
      segments <- Seg()
      
      # Use for each time point the complete structure (from time point 1)
      if(input$updateSeg) segments <- segments[segments$time_in_serie == input$slider1,]
      else segments <- segments[segments$time_in_serie == 1,]
      reporter <- reporter[reporter$time_in_serie == input$slider1,]
      
      # transform pixels into cm:
      reporter <- reporter %>% 
        mutate(depth = rep_y/138.6, width=rep_x/138,6)
      segments <- segments %>% 
        mutate(start_depth = start_y/138.6, start_width=start_x/138.6, end_depth = end_y/138.6, end_width=end_x/138.6)
      
      # plot the reporters
      ggplot(reporter, aes(x = width, y = desc(depth), size = intensity)) +
        geom_segment(data=segments,
                     aes(x = start_width, y = -start_depth, xend = end_width, yend = -end_depth), 
                     size=0.5, colour="grey", show_guide=F) + 
        geom_point(colour = "magenta", alpha=0.7) +
        coord_fixed(ratio = 1) +
        theme_bw() 
      
        
    })
    
  }
)