import React, { ReactElement } from 'react';

import Navigation from '../Navigation/Navigation';
import Notification from '../Notification/Notification';
import { Drawer, Theme, Toolbar } from '@material-ui/core';
import { createStyles, makeStyles } from '@material-ui/core/styles';

interface Props {
  children: string | ReactElement | ReactElement[];
  sidebarContent: string | ReactElement | ReactElement[];
  setSidebarContent: Function;
  pageTitle?: string;
  logo?: string;
}

const useStyles = makeStyles((theme: Theme) =>
  createStyles({
    root: {
      display: 'flex',
    },
    appBar: {
      zIndex: theme.zIndex.drawer + 1,
      position: 'relative',
      backgroundColor: theme.palette.primary.main,
    },
    drawer: {
      flexShrink: 0,
    },
    drawerPaper: {
      width: 320,
      backgroundColor: 'transparent',
      border: 'none',
      maxHeight: '100%',
      height: 'inherit',
    },
    drawerContainer: {
      overflow: 'auto',
    },
    content: {
      display: 'flex',
      flexDirection: 'row',
      padding: theme.spacing(3),
    },
  })
);

/**
 * Contains our site layout, Navigation on top, content below
 * @param param0
 */
const Layout: React.FC<Props> = ({
  children,
  sidebarContent,
  setSidebarContent,
  pageTitle,
  logo,
}) => {
  const classes = useStyles();

  return (
    <React.Fragment>
      <Navigation
        appBarClass={classes.appBar}
        logo={logo}
        setSidebarContent={setSidebarContent}
        pageTitle={pageTitle}
      />
      <div className={classes.content}>
        {children}
        <Drawer
          className={classes.drawer}
          variant='permanent'
          anchor={'right'}
          classes={{
            paper: classes.drawerPaper,
          }}
          color={'secondary'}
        >
          <Toolbar />
          {sidebarContent}
        </Drawer>
      </div>

      <Notification />
    </React.Fragment>
  );
};

export default Layout;
