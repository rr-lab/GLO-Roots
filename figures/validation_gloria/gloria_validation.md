# Validation of GLO-RIA
Rubén Rellán-Álvarez, Guillaume Lobet, Heike Lindner, Pierre-Luc Pradier, Jose Sebastian1, Muh-Ching Yee, Yu Geng, Charlotte Trontin, Therese LaRue, Amanda Schrager, Cara Haney, Rita Nieu, Julin Maloof, John P. Vogel, José R. Dinneny  
17 Jul 2015  

This document presents the validation of different GLO-RIA measurments. Two aproaches were chosen for the validation. First, we compared some of the measurements from GLO-RIA with measurements done manually. Second, we used the ArchiSimple model (Pagès et al. 2013) to generated images of root systems. Since the images are created using a model, we know the ground-truth values for each of them. This allows us to validate GLORIA on a large number of ground-truth images.




# Validation using manual measurements of real images

This validation was carried out in an independent set of 15 images of different arabidopsis accesions grown in control conditions.

Root system depth and width and individual lateral root angles were measured in the images and then GLO-RIA was used to measure the same parameters.

## Directionality


```r
man_gloria <- read.csv("manual_gloria.csv")

fit <- lm(man_gloria$man_dir ~ man_gloria$gloria_dir)
r2 <- round(summary(fit)$r.squared, 3)
sp <- round(rcorr(man_gloria$gloria_dir,man_gloria$man_dir, type="spearman")$r[1,2], 3)
pe <- round(rcorr(man_gloria$gloria_dir,man_gloria$man_dir, type="pearson")$r[1,2], 3)
leg <- paste("r-squared = ",r2, "\nPearson = ",pe, "\nSpearman = ",sp)

plot_man_dir <- ggplot(man_gloria, aes(gloria_dir, man_dir)) + 
  geom_point(alpha=0.5) + 
  geom_smooth(method='lm') +
  xlab("Mean manual directionality [°]") + ylab("Mean estimated directionality [°]") + 
  theme_bw() +
  annotate("text", y= max(man_gloria$man_dir), x =max(man_gloria$gloria_dir), 
           label=leg, hjust=1, vjust=5, size=5) +
  theme(plot.title=element_text(hjust=0, size=30)) + 
  ggsave(file="figures/man-dir.pdf", width=6, height=6)
```

## Depth


```r
fit <- lm(man_gloria$man_d ~ man_gloria$gloria_d)
r2 <- round(summary(fit)$r.squared, 3)
sp <- round(rcorr(man_gloria$gloria_d,man_gloria$man_d, type="spearman")$r[1,2], 3)
pe <- round(rcorr(man_gloria$gloria_d,man_gloria$man_d, type="pearson")$r[1,2], 3)
leg <- paste("r-squared = ",r2, "\nPearson = ",pe, "\nSpearman = ",sp)

plot_man_d <- ggplot(man_gloria, aes(gloria_d, man_d)) + 
  geom_point(alpha=0.5) + 
  geom_smooth(method='lm') +
  xlab("Manual depth [cm]") + ylab("Estimated depth [cm]") + 
  theme_bw() +
  annotate("text", y= max(man_gloria$man_d), x =max(man_gloria$gloria_d), 
           label=leg, hjust=1, vjust=5, size=5) +
  theme(plot.title=element_text(hjust=0, size=30)) + 
  ggsave(file="figures/man-depth.pdf", width=6, height=6)
```

## Width


```r
fit <- lm(man_gloria$man_w ~ man_gloria$gloria_w)
r2 <- round(summary(fit)$r.squared, 3)
sp <- round(rcorr(man_gloria$gloria_w,man_gloria$man_w, type="spearman")$r[1,2], 3)
pe <- round(rcorr(man_gloria$gloria_w,man_gloria$man_w, type="pearson")$r[1,2], 3)
leg <- paste("r-squared = ",r2, "\nPearson = ",pe, "\nSpearman = ",sp)

plot_man_w <- ggplot(man_gloria, aes(gloria_w, man_w)) + 
  geom_point(alpha=0.5) + 
  geom_smooth(method='lm') +
  xlab("Manual width [cm]") + ylab("Estimated width [cm]") + 
  theme_bw() +
  annotate("text", y= max(man_gloria$man_w), x =max(man_gloria$gloria_w), 
           label=leg, hjust=1, vjust=5, size=5) +
  theme(plot.title=element_text(hjust=0, size=30)) + 
  ggsave(file="figures/man-width.pdf", width=6, height=6)
```

grid.arrange(plot_man_dir, plot_man_d, plot_man_w, ncol=3) 

# Validation using ArchiSimple model generated images

# Directionality



For the validation of directionality measurements, we used Archisimple to generate root systems with contrasted gravitropism. We compared the results from GLO-RIA with the ground-truth values from the model.





![](gloria_validation_files/figure-html/plot_dir-1.png) 

## Size related measurements




For the validation of size variables, we used Archisimple to generate 1050 root systems with contrasted size. We compared the results from GLO-RIA with the ground-truth values from the model.

## Depth



## Width



```


# References 

Pagès, L., Bécel, C., Boukcim, H., Moreau, D., Nguyen, C., & Voisin, A.-S. (2013). Calibration and evaluation of ArchiSimple, a simple model of root system architecture, 290, 76–84. http://doi.org/10.1016/j.ecolmodel.2013.11.014
