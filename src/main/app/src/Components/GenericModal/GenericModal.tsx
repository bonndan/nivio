import React, { ReactElement, useState, useEffect } from 'react';
import Modal from 'react-modal';

interface IGenericModalProps {
  modalContent: string | ReactElement | ReactElement[] | null;
}

/**
 * CURRENTLY UNUSED
 * Creates a React Modal with given modalContent, that can be closed via button, esc or by clicking outside of the overlay
 * @param modalContent Content that should be displayed
 */
const GenericModal: React.FC<IGenericModalProps> = ({ modalContent }) => {
  Modal.setAppElement(document.body);

  const [showModal, setShowModal] = useState(false);

  // We have to save modalContent in a state independent of our props to use shouldCloseOn functions from react-modal
  // If we just check our prop modalContent it wont be null until WE tell it to outside of our GenericModal
  // -> showModal would always be true
  const [modalContentState, setModalContentState] = useState(modalContent);

  useEffect(() => {
    if (modalContent) {
      setModalContentState(modalContent);
    } else {
      setModalContentState(null);
    }
  }, [modalContent]);

  useEffect(() => {
    if (modalContentState) {
      setShowModal(true);
    } else {
      setShowModal(false);
    }
  }, [modalContentState]);

  const closeModal = () => {
    setShowModal(false);
    setModalContentState(null);
  };

  return (
    <Modal
      isOpen={showModal}
      className='Modal'
      overlayClassName='Overlay'
      shouldCloseOnEsc={true}
      shouldCloseOnOverlayClick={true}
      onRequestClose={closeModal}
      contentLabel='Modal'
    >
      <button className={'close button'} onClick={closeModal}>
        close
      </button>
      {modalContentState}
    </Modal>
  );
};

export default GenericModal;
