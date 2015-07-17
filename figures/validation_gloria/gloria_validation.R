
library(plyr)
library(ggplot2)
library(gridExtra)
library(data.table)
library(Hmisc)

gloria <- fread("gloria-data.csv", header = T) # The estimators
gloria$angle <- abs(gloria$angle)

rsml <- fread("rsml-data.csv", header = T) # The estimators
rsml$theta <- rsml$theta * (180 / pi)
rsml$theta <- rsml$theta - 270
rsml <- rsml[rsml$theta <= 90 & rsml$theta >= -90,]
rsml$theta <- abs(rsml$theta)

rsml$image <- as.character(rsml$image)
gloria$image <- as.character(gloria$image)
gloria$image <- substr(gloria$image, 0, nchar(gloria$image)-4)
ims <- unique(rsml$image)
ims <- ims[!is.na(ims)]



# Loop over the parameters space
counter <- 0
tot_sim <- length(ims)
percent <- 0
mean.rs <- data.frame(id=character(),sr=numeric(), gl1=numeric())

# create a new table with the mean value per image
for(i in ims){
  counter <- counter+1
  prog <- ( counter / tot_sim ) * 100
  if(prog > percent){
    message(paste(round(prog), "% of data analysed"))
    percent <- percent + 5
  }
  temp <- gloria[gloria$image == i]
  
  # Get the mean angle from the distribution
  temp <- data.frame(id=i, 
                     sr=mean(rsml$theta[rsml$image == i]),#sum(abs(h2$angle) * h1$count) / sum(h1$count), 
                     gl1=sum(temp$angle * temp$count) / sum(temp$count))  
  mean.rs <- rbind(mean.rs, temp)
}


# Remove outliyers
b1 <- boxplot(mean.rs$sr, plot=F)
b2 <- boxplot(mean.rs$gl1, plot=F)
temp <- mean.rs[mean.rs$sr < b1$stats[5,],]


# Look at the relation between the directionality and the root orientation
fit <- lm(temp$gl1 ~ temp$sr)
r2 <- round(summary(fit)$r.squared, 3)
sp <- round(rcorr(temp$sr,temp$gl1, type="spearman")$r[1,2], 3)
pe <- round(rcorr(temp$sr,temp$gl1, type="pearson")$r[1,2], 3)
leg <- paste("r-squared = ",r2, "\nPearson = ",pe, "\nSpearman = ",sp)

plot1 <- ggplot(temp, aes(sr, gl1)) + 
  geom_point(alpha=0.5) + 
  geom_smooth(method='lm') +
  ylab("Mean estimated directionality [°]") + 
  xlab("Mean root orientation [°]") + 
  theme_bw() +
  annotate("text", y= min(temp$gl1), x =max(temp$sr), 
           label=leg, hjust=1, vjust=0, size=5)


temp$id <- as.character(temp$id)
for(i in 1:nrow(temp)){
  temp$gravi[i] <- as.numeric(strsplit(temp$id[i], "-")[[1]][6])
  temp$insert[i] <- as.numeric(strsplit(temp$id[i], "-")[[1]][11])
}

# Look at the relation with the gravitropism (keep only one insertion angle)
temp <- temp[temp$insert == 1.5,]
fit <- lm(temp$gl1 ~ temp$gravi)
r2 <- round(summary(fit)$r.squared, 3)
sp <- round(rcorr(temp$gravi,temp$gl1, type="spearman")$r[1,2], 3)
pe <- round(rcorr(temp$gravi,temp$gl1, type="pearson")$r[1,2], 3)
leg <- paste("r-squared = ",r2, "\nPearson = ",pe, "\nSpearman = ",sp)

plot2 <- ggplot(temp, aes(gravi, gl1)) + 
  geom_point(alpha=0.5) + 
  geom_smooth(method='lm') +
  xlab("Gravitropism strength [-]") + ylab("Mean estimated directionality [°]") + 
  theme_bw() +
  annotate("text", y= max(temp$gl1), x =max(temp$gravi), 
           label=leg, hjust=1, vjust=1, size=5)

manual_gloria <- read.csv("manual_gloria.csv")

fit <- lm(manual_gloria$manual ~ manual_gloria$gloria)
r2 <- round(summary(fit)$r.squared, 3)
sp <- round(rcorr(manual_gloria$gloria,manual_gloria$manual, type="spearman")$r[1,2], 3)
pe <- round(rcorr(manual_gloria$gloria,manual_gloria$manual, type="pearson")$r[1,2], 3)
leg <- paste("r-squared = ",r2, "\nPearson = ",pe, "\nSpearman = ",sp)

plot3 <- ggplot(manual_gloria, aes(gloria, manual)) + 
  geom_point(alpha=0.5) + 
  geom_smooth(method='lm') +
  xlab("Mean manual directionality [°]") + ylab("Mean estimated directionality [°]") + 
  theme_bw() +
  annotate("text", y= max(manual_gloria$manual), x =max(manual_gloria$gloria), 
           label=leg, hjust=1, vjust=5, size=5)

grid.arrange(plot1, plot2, plot3, ncol=2)



