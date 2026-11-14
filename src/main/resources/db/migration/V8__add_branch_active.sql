-- A branch can be closed: inactive branches are not offered for booking (see BranchRepository/ReservationService)
ALTER TABLE branches ADD COLUMN active BOOLEAN NOT NULL DEFAULT true;
