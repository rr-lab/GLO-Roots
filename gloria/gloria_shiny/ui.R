
library(shiny)

shinyUI(fluidPage(
  
  # Application title
  titlePanel(h1("GLO-RIA")),
  
  # Sidebar with a slider input for the number of bins
  sidebarLayout(
    sidebarPanel(    
      
      fileInput('rep_file', 'Choose Reporter File', accept=c('text/csv', 'text/comma-separated-values,text/plain', '.csv')),
      
      fileInput('seg_file', 'Choose Segment File', accept=c('text/csv', 'text/comma-separated-values,text/plain', '.csv')),
      
      sliderInput("slider1", label = h3("Time in serie"), min = 1, max = 5, value = 1),
      
      checkboxInput("updateSeg", label = "Update segment data", value = T),
        
      tags$hr(),
      
      img(src = "gloria_logo.png", width = 200)
    ),
    
    # Show a plot of the generated distribution
    mainPanel(
      tabsetPanel(     
        tabPanel("Plot Root Reporter",
                 plotOutput("distPlot"),
                 value=1
        ),
        id="tabs1"
      )
    )
  )
))