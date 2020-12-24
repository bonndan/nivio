import React from 'react';
import { Typography, AppBar, Theme, createStyles, Box, darken } from '@material-ui/core';
import { Link } from 'react-router-dom';

import Toolbar from '@material-ui/core/Toolbar';

import makeStyles from '@material-ui/core/styles/makeStyles';
import Search from '../Landscape/Search/Search';
import { HelpOutlineRounded, HomeOutlined } from '@material-ui/icons';
import IconButton from '@material-ui/core/IconButton';

const useStyles = makeStyles((theme: Theme) =>
  createStyles({
    grow: {
      flexGrow: 1,
    },
    pageTitle: {
      padding: 11,
      paddingLeft: 16,
      paddingRight: 16,
      backgroundColor: darken(theme.palette.secondary.main, 0.2),
    },
    menuIcon: {
      color: 'rgba(255, 255, 255, 0.75)',
    },
  })
);

interface Props {
  appBarClass: string;
  setSidebarContent: Function;
  findFunction: Function;
  pageTitle?: string;
}

/**
 * Header Component
 */
/*
    value         |0px     600px    960px    1280px   1920px
    key           |xs      sm       md       lg       xl
    screen width  |--------|--------|--------|--------|-------->
    range         |   xs   |   sm   |   md   |   lg   |   xl
     */
const Navigation: React.FC<Props> = ({
  appBarClass,
  setSidebarContent,
  findFunction,
  pageTitle,
}) => {
  const classes = useStyles();

  return (
    <AppBar position='static' className={appBarClass}>
      <Toolbar variant='dense'>
        <Box className={classes.pageTitle}>
          <Typography variant='h6'>nivio: {pageTitle}</Typography>
        </Box>
        <div className={classes.grow} />
        <Search findItem={findFunction} setSidebarContent={setSidebarContent} />
        <IconButton component={Link} to={``} className={classes.menuIcon}>
          <HomeOutlined />
        </IconButton>{' '}
        <IconButton
          className={classes.menuIcon}
          data-testid='ManualButton'
          component={Link}
          to={`/man/install.html`}
          title={'Help / Manual'}
        >
          <HelpOutlineRounded />
        </IconButton>
      </Toolbar>
    </AppBar>
  );
};

export default Navigation;
